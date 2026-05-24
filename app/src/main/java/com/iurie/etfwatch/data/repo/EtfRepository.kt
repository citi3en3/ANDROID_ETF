package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.EtfDao
import com.iurie.etfwatch.data.db.EtfEntity
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.scrape.HamiltonScraper
import com.iurie.etfwatch.data.seed.SeedLoader
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtfRepository @Inject constructor(
    private val etfDao: EtfDao,
    private val quoteRepo: QuoteRepository,
    private val fmp: FmpService,
    private val scraper: HamiltonScraper,
    private val seedLoader: SeedLoader,
) {

    suspend fun ensureSeeded() = seedLoader.seedIfNeeded()

    fun watchlist(): Flow<List<EtfWithQuote>> = etfDao.watchlistFlow()
    fun hamilton(): Flow<List<EtfWithQuote>> = etfDao.hamiltonFlow()
    fun leveraged(): Flow<List<EtfWithQuote>> = etfDao.leveragedFlow()
    fun detail(ticker: String): Flow<EtfWithQuote?> = etfDao.withQuoteFlow(ticker)

    suspend fun addToWatchlist(ticker: String, name: String?, exchange: String?) {
        val existing = etfDao.byTicker(ticker)
        if (existing == null) {
            etfDao.upsert(
                EtfEntity(
                    ticker = ticker,
                    name = name ?: ticker,
                    exchange = exchange ?: "",
                    isUserAdded = true,
                    isWatchlist = true,
                )
            )
        } else {
            etfDao.setWatchlist(ticker, true)
        }
        quoteRepo.refresh(listOf(ticker))
    }

    suspend fun removeFromWatchlist(ticker: String) {
        etfDao.setWatchlist(ticker, false)
    }

    suspend fun search(query: String) = runCatching {
        fmp.search(query)
    }.onFailure { Timber.w(it, "FMP search failed") }.getOrDefault(emptyList())

    suspend fun refreshAll() {
        ensureSeeded()
        val tickers = etfDao.tickersToRefresh()
        if (tickers.isNotEmpty()) quoteRepo.refresh(tickers)
        refreshHamiltonYields()
    }

    private suspend fun refreshHamiltonYields() {
        val scraped = scraper.fetch()
        if (scraped.isEmpty()) return
        scraped.forEach { s ->
            val existing = etfDao.byTicker(s.ticker) ?: return@forEach
            if (s.yieldPct != null) {
                quoteRepo.mergeDividendYield(existing.ticker, s.yieldPct)
            }
        }
    }
}
