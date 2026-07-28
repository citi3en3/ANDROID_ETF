package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.QuoteDao
import com.iurie.etfwatch.data.db.QuoteEntity
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.remote.HistoricalPoint
import com.iurie.etfwatch.ui.common.ChartPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepository @Inject constructor(
    private val quoteDao: QuoteDao,
    private val fmp: FmpService,
) {

    /** FMP supports comma-separated symbols on /quote — batch to keep the call count down. */
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
                    if (rows.isNotEmpty()) quoteDao.upsertAll(rows)
                }
                .onFailure { Timber.w(it, "FMP quotes batch failed for $chunk") }
        }
    }

    suspend fun history(ticker: String, days: Int): List<ChartPoint> {
        val to = LocalDate.now(ZoneOffset.UTC)
        val from = to.minusDays(days.toLong())
        return runCatching { fmp.historical(ticker, from.toString(), to.toString()) }
            .onFailure { Timber.w(it, "FMP historical failed for $ticker") }
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

    /** Fetches several tickers' history concurrently, bounded so a big watchlist can't flood FMP. */
    suspend fun histories(tickers: List<String>, days: Int): Map<String, List<ChartPoint>> =
        coroutineScope {
            if (tickers.isEmpty()) return@coroutineScope emptyMap()
            val gate = Semaphore(MAX_CONCURRENT_HISTORY)
            tickers.distinct()
                .map { t -> async { t to gate.withPermit { history(t, days) } } }
                .awaitAll()
                .filter { it.second.isNotEmpty() }
                .toMap()
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
     * One call per ticker (FMP has no batch historical endpoint), bounded-parallel.
     */
    suspend fun refreshReturnsFromFmp(tickers: List<String>) = coroutineScope {
        if (tickers.isEmpty()) return@coroutineScope
        val now = System.currentTimeMillis()
        val to = LocalDate.now(ZoneOffset.UTC)
        val from = to.minusDays(HISTORY_LOOKBACK_DAYS) // buffer past 60 days to cover weekends/holidays
        val nowEpochDay = to.toEpochDay()
        val gate = Semaphore(MAX_CONCURRENT_HISTORY)

        val rows = tickers.map { t ->
            async {
                runCatching {
                    val existing = quoteDao.byTicker(t) ?: return@async null
                    val price = existing.price ?: return@async null
                    val series = gate.withPermit {
                        fmp.historical(t, from.toString(), to.toString()).historical.orEmpty()
                    }.mapNotNull { p ->
                        runCatching { LocalDate.parse(p.date.take(10)).toEpochDay() to p.close }.getOrNull()
                    }
                    if (series.isEmpty()) return@async null

                    fun ret(daysAgo: Long) =
                        ReturnCalculator.returnPct(series, nowEpochDay, daysAgo, price)

                    existing.copy(
                        monthReturnPct = ret(30) ?: existing.monthReturnPct,
                        twoMonthReturnPct = ret(60) ?: existing.twoMonthReturnPct,
                        week1ReturnPct = ret(7) ?: existing.week1ReturnPct,
                        week2ReturnPct = ret(14) ?: existing.week2ReturnPct,
                        week3ReturnPct = ret(21) ?: existing.week3ReturnPct,
                        week5ReturnPct = ret(35) ?: existing.week5ReturnPct,
                        updatedAt = now,
                    )
                }.onFailure { Timber.w(it, "FMP historical failed for $t") }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        if (rows.isNotEmpty()) quoteDao.upsertAll(rows)
    }

    companion object {
        private const val BATCH_SIZE = 50
        private const val HISTORY_LOOKBACK_DAYS = 75L

        /** Keeps a large watchlist from opening one connection per ticker at once. */
        private const val MAX_CONCURRENT_HISTORY = 8

        private fun formatChartDate(raw: String): String = runCatching {
            val d = LocalDate.parse(raw.take(10))
            "%02d/%02d".format(Locale.US, d.monthValue, d.dayOfMonth)
        }.getOrDefault(raw)
    }
}
