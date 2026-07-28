package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.EtfDao
import com.iurie.etfwatch.data.db.EtfEntity
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.filter.EtnFilter
import com.iurie.etfwatch.data.remote.FmpService
import com.iurie.etfwatch.data.remote.FmpSymbols
import com.iurie.etfwatch.data.remote.SearchHit
import com.iurie.etfwatch.data.scrape.HamiltonScraper
import com.iurie.etfwatch.data.seed.SeedLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a watchlist add, so the UI can explain a refusal instead of silently doing nothing. */
sealed interface AddResult {
    data class Added(val ticker: String) : AddResult
    data class Rejected(val ticker: String, val reason: String) : AddResult
}

@Singleton
class EtfRepository @Inject constructor(
    private val etfDao: EtfDao,
    private val quoteRepo: QuoteRepository,
    private val fmp: FmpService,
    private val scraper: HamiltonScraper,
    private val seedLoader: SeedLoader,
) {

    /**
     * Refreshes read-modify-write the same quote rows, so two overlapping passes can clobber each
     * other's freshly written return columns. One at a time.
     */
    private val refreshMutex = Mutex()

    suspend fun ensureSeeded() {
        seedLoader.seedIfNeeded()
        migrateLegacySymbols()
        purgeEtns()
    }

    /**
     * ETNs are never tracked. Installs seeded before that rule (or watchlists built from older
     * search results) can still hold one, so drop them — and their quotes — on every seed pass.
     */
    private suspend fun purgeEtns() {
        val etns = etfDao.all().filter { EtnFilter.isEtn(it.ticker, it.name) }.map { it.ticker }
        if (etns.isEmpty()) return
        etfDao.deleteTickers(etns)
        quoteRepo.deleteQuotes(etns)
        Timber.i("Purged ${etns.size} ETN(s): $etns")
    }

    /**
     * Earlier builds stored TSX USD units as `BASE.U.TO`, which FMP answers with `[]` — those rows
     * could never get a price. Move them onto the working `BASE-U.TO` spelling, keeping any
     * watchlist flag the user had set.
     */
    private suspend fun migrateLegacySymbols() {
        val legacy = etfDao.all().filter { FmpSymbols.isLegacyUsdUnit(it.ticker) }
        if (legacy.isEmpty()) return
        legacy.forEach { old ->
            val fixed = FmpSymbols.normalize(old.ticker)
            if (fixed == old.ticker) return@forEach
            val current = etfDao.byTicker(fixed)
            if (current == null) {
                etfDao.upsert(old.copy(ticker = fixed))
            } else if (old.isWatchlist && !current.isWatchlist) {
                etfDao.setWatchlist(fixed, true)
            }
        }
        val stale = legacy.map { it.ticker }
        etfDao.deleteTickers(stale)
        quoteRepo.deleteQuotes(stale)
        Timber.i("Migrated ${stale.size} legacy USD-unit symbol(s): $stale")
    }

    fun watchlist(): Flow<List<EtfWithQuote>> = etfDao.watchlistFlow()
    fun hamilton(): Flow<List<EtfWithQuote>> = etfDao.hamiltonFlow()
    fun leveraged(): Flow<List<EtfWithQuote>> = etfDao.leveragedFlow()
    fun detail(ticker: String): Flow<EtfWithQuote?> = etfDao.withQuoteFlow(ticker)

    suspend fun addToWatchlist(ticker: String, name: String?, exchange: String?): AddResult {
        val symbol = FmpSymbols.normalize(ticker)
        if (EtnFilter.isEtn(symbol, name)) {
            Timber.i("Refused to watch $symbol — ETNs are not tracked")
            return AddResult.Rejected(symbol, "$symbol is an ETN — this app tracks ETFs only")
        }
        val existing = etfDao.byTicker(symbol)
        // Anything already seeded is a known ETF; only unknown symbols need the type check.
        if (existing == null && isEtf(symbol) == false) {
            Timber.i("Refused to watch $symbol — not an ETF")
            return AddResult.Rejected(symbol, "$symbol is not an ETF")
        }
        if (existing == null) {
            etfDao.upsert(
                EtfEntity(
                    ticker = symbol,
                    name = name ?: symbol,
                    exchange = exchange ?: "",
                    isUserAdded = true,
                    isWatchlist = true,
                )
            )
        } else {
            etfDao.setWatchlist(symbol, true)
        }
        quoteRepo.refresh(listOf(symbol))
        return AddResult.Added(symbol)
    }

    /**
     * `true`/`false` from FMP's profile, or `null` when the answer isn't available. A network or
     * plan failure must not block the user from adding a ticker, so callers treat null as "allow".
     */
    private suspend fun isEtf(ticker: String): Boolean? = runCatching {
        fmp.profile(ticker).firstOrNull()?.isEtf
    }.onFailure { Timber.w(it, "FMP profile failed for $ticker") }.getOrNull()

    suspend fun removeFromWatchlist(ticker: String) {
        etfDao.setWatchlist(ticker, false)
    }

    suspend fun search(query: String): List<SearchHit> = runCatching {
        fmp.search(query).filterNot { EtnFilter.isEtn(it.symbol, it.name) }
    }.onFailure { Timber.w(it, "FMP search failed") }.getOrDefault(emptyList())

    suspend fun refreshAll() = refreshMutex.withLock {
        ensureSeeded()
        // Scrape first: it can introduce Hamilton tickers that aren't in the DB yet, and they
        // should be priced in this pass rather than sitting blank until the next one.
        refreshHamiltonYields()
        val tickers = etfDao.tickersToRefresh()
        if (tickers.isNotEmpty()) {
            quoteRepo.refresh(tickers)
            quoteRepo.refreshReturnsFromFmp(tickers)
        }
    }

    suspend fun refreshHamilton() = refreshMutex.withLock {
        ensureSeeded()
        refreshHamiltonYields()
        val hamiltonTickers = etfDao.all().filter { it.isHamilton }.map { it.ticker }
        if (hamiltonTickers.isNotEmpty()) {
            quoteRepo.refresh(hamiltonTickers)
            quoteRepo.refreshReturnsFromFmp(hamiltonTickers)
        }
    }

    private suspend fun refreshHamiltonYields() {
        val scraped = scraper.fetch().filterNot { EtnFilter.isEtn(it.ticker, it.name) }
        if (scraped.isEmpty()) return
        scraped.forEach { s ->
            if (etfDao.byTicker(s.ticker) == null) {
                etfDao.upsert(
                    EtfEntity(
                        ticker = s.ticker,
                        name = s.name ?: s.ticker,
                        exchange = "TSX",
                        sector = s.sector,
                        isHamilton = true,
                    )
                )
            }
            if (s.yieldPct != null) {
                quoteRepo.mergeDividendYield(s.ticker, s.yieldPct)
            }
        }
    }
}
