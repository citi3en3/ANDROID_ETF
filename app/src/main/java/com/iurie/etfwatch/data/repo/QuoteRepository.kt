package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.QuoteDao
import com.iurie.etfwatch.data.db.QuoteEntity
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.remote.HistoricalPoint
import com.iurie.etfwatch.ui.common.ChartPoint
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepository @Inject constructor(
    private val quoteDao: QuoteDao,
    private val fmp: FmpService,
) {

    /** FMP supports comma-separated symbols on /quote — batch to stay within free-tier limits. */
    suspend fun refresh(tickers: List<String>) {
        if (tickers.isEmpty()) return
        tickers.chunked(BATCH_SIZE).forEach { chunk ->
            runCatching { fmp.quotes(chunk.joinToString(",")) }
                .onSuccess { quotes ->
                    val now = System.currentTimeMillis()
                    val rows = quotes.map { q ->
                        // copy() so the separately-refreshed return columns survive a quote refresh.
                        val existing = quoteDao.byTicker(q.symbol)
                        existing?.copy(
                            price = q.price,
                            changePct = q.changesPercentage,
                            marketCap = q.marketCap,
                            updatedAt = now,
                        ) ?: QuoteEntity(
                            ticker = q.symbol,
                            price = q.price,
                            changePct = q.changesPercentage,
                            dividendYield = null,
                            marketCap = q.marketCap,
                            updatedAt = now,
                        )
                    }
                    quoteDao.upsertAll(rows)
                }
                .onFailure { Timber.w(it, "FMP quotes batch failed for $chunk") }
        }
    }

    suspend fun history(ticker: String, days: Int): List<ChartPoint> {
        val to = LocalDate.now(ZoneOffset.UTC)
        val from = to.minusDays(days.toLong())
        return runCatching { fmp.historical(ticker, from.toString(), to.toString()) }
            .getOrNull()
            ?.historical
            ?.sortedBy { it.date }
            ?.mapIndexed { idx: Int, p: HistoricalPoint ->
                val close = p.close.toFloat()
                ChartPoint(
                    x = idx.toFloat(),
                    open = p.open?.toFloat() ?: close,
                    high = p.high?.toFloat() ?: close,
                    low = p.low?.toFloat() ?: close,
                    close = close,
                    label = formatChartDate(p.date),
                )
            }
            .orEmpty()
    }

    suspend fun deleteQuotes(tickers: List<String>) {
        if (tickers.isNotEmpty()) quoteDao.deleteTickers(tickers)
    }

    suspend fun mergeDividendYield(ticker: String, yieldPct: Double) {
        val existing = quoteDao.byTicker(ticker)
        val now = System.currentTimeMillis()
        quoteDao.upsertAll(
            listOf(
                existing?.copy(dividendYield = yieldPct, updatedAt = now)
                    ?: QuoteEntity(ticker, null, null, yieldPct, null, now)
            )
        )
    }

    /**
     * Fetches 1W/2W/3W/5W/1M/2M return from FMP's historical daily closes.
     * One call per ticker (FMP has no batch historical endpoint), parallelized via async/awaitAll.
     */
    suspend fun refreshReturnsFromFmp(tickers: List<String>) = coroutineScope {
        if (tickers.isEmpty()) return@coroutineScope
        val now = System.currentTimeMillis()
        val to = LocalDate.now(ZoneOffset.UTC)
        val from = to.minusDays(75) // buffer past 60 days to cover weekends/holidays
        val nowEpochDay = to.toEpochDay()
        val rows = tickers.map { t ->
            async {
                runCatching {
                    val existing = quoteDao.byTicker(t) ?: return@async null
                    val price = existing.price ?: return@async null
                    val series = fmp.historical(t, from.toString(), to.toString()).historical.orEmpty()
                        .mapNotNull { p -> runCatching { LocalDate.parse(p.date.take(10)).toEpochDay() to p.close }.getOrNull() }
                    if (series.isEmpty()) return@async null
                    fun returnSince(daysAgo: Long): Double? {
                        val targetDay = nowEpochDay - daysAgo
                        val close = series.minByOrNull { kotlin.math.abs(it.first - targetDay) }?.second
                        return if (close != null && close > 0.0) ((price - close) / close) * 100.0 else null
                    }
                    existing.copy(
                        monthReturnPct = returnSince(30) ?: existing.monthReturnPct,
                        twoMonthReturnPct = returnSince(60) ?: existing.twoMonthReturnPct,
                        week1ReturnPct = returnSince(7) ?: existing.week1ReturnPct,
                        week2ReturnPct = returnSince(14) ?: existing.week2ReturnPct,
                        week3ReturnPct = returnSince(21) ?: existing.week3ReturnPct,
                        week5ReturnPct = returnSince(35) ?: existing.week5ReturnPct,
                        updatedAt = now,
                    )
                }.onFailure { Timber.w(it, "FMP historical failed for $t") }.getOrNull()
            }
        }.awaitAll().filterNotNull()
        if (rows.isNotEmpty()) quoteDao.upsertAll(rows)
    }

    companion object {
        private const val BATCH_SIZE = 50

        private fun formatChartDate(raw: String): String = runCatching {
            val d = LocalDate.parse(raw.take(10))
            "%02d/%02d".format(d.monthValue, d.dayOfMonth)
        }.getOrDefault(raw)
    }
}
