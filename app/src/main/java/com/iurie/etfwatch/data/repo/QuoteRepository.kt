package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.QuoteDao
import com.iurie.etfwatch.data.db.QuoteEntity
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.remote.HistoricalPoint
import com.iurie.etfwatch.data.remote.YahooService
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
    private val yahoo: YahooService,
) {

    /** FMP supports comma-separated symbols on /quote — batch to stay within free-tier limits. */
    suspend fun refresh(tickers: List<String>) {
        if (tickers.isEmpty()) return
        tickers.chunked(BATCH_SIZE).forEach { chunk ->
            runCatching { fmp.quotes(chunk.joinToString(",")) }
                .onSuccess { quotes ->
                    val now = System.currentTimeMillis()
                    val rows = quotes.map { q ->
                        val existing = quoteDao.byTicker(q.symbol)
                        QuoteEntity(
                            ticker = q.symbol,
                            price = q.price,
                            changePct = q.changesPercentage,
                            dividendYield = existing?.dividendYield,
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

    /** Fetches price + day change + 1-month return from Yahoo Finance. */
    suspend fun refreshPricesFromYahoo(tickers: List<String>) = coroutineScope {
        if (tickers.isEmpty()) return@coroutineScope
        val now = System.currentTimeMillis()
        val rows = tickers.map { t ->
            async {
                runCatching {
                    val result = yahoo.chart(t).chart?.result?.firstOrNull() ?: return@async null
                    val meta = result.meta ?: return@async null
                    val price = meta.regularMarketPrice ?: return@async null
                    val prev = meta.previousClose ?: meta.chartPreviousClose
                    val changePct = if (prev != null && prev > 0.0) ((price - prev) / prev) * 100.0 else null
                    val closes = result.indicators?.quote?.firstOrNull()?.close?.filterNotNull().orEmpty()
                    val firstClose = closes.firstOrNull()
                    val monthReturn = if (firstClose != null && firstClose > 0.0) ((price - firstClose) / firstClose) * 100.0 else null
                    val existing = quoteDao.byTicker(t)
                    existing?.copy(
                        price = price,
                        changePct = changePct,
                        monthReturnPct = monthReturn ?: existing.monthReturnPct,
                        updatedAt = now,
                    ) ?: QuoteEntity(t, price, changePct, null, null, now, monthReturn)
                }.onFailure { Timber.w(it, "Yahoo chart failed for $t") }.getOrNull()
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
