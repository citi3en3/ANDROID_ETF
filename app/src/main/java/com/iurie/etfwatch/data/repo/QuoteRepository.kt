package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.QuoteDao
import com.iurie.etfwatch.data.db.QuoteEntity
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.remote.HistoricalPoint
import com.iurie.etfwatch.ui.common.ChartPoint
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
            ?.mapIndexed { idx: Int, p: HistoricalPoint -> ChartPoint(idx.toFloat(), p.close.toFloat()) }
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

    companion object {
        private const val BATCH_SIZE = 50
    }
}
