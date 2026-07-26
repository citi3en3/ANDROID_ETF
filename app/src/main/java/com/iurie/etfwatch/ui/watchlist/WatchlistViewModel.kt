package com.iurie.etfwatch.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.remote.SearchHit
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.ui.common.SortMode
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val items: List<EtfWithQuote> = emptyList(),
    val sort: SortMode = SortMode.Ticker,
    val isRefreshing: Boolean = false,
    val searchResults: List<SearchHit> = emptyList(),
)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repo: EtfRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {

    private val sort = MutableStateFlow(SortMode.Ticker)
    private val refreshing = MutableStateFlow(false)
    private val searchResults = MutableStateFlow<List<SearchHit>>(emptyList())

    val state: StateFlow<WatchlistUiState> =
        combine(repo.watchlist(), sort, refreshing, searchResults) { items, s, r, sr ->
            WatchlistUiState(items = items.sortedBy(s), sort = s, isRefreshing = r, searchResults = sr)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun setSort(s: SortMode) { sort.value = s }

    fun refresh() {
        refreshing.value = true
        scheduler.runOnceNow()
        // Worker reports back via DB flow; flip flag after small grace.
        viewModelScope.launch {
            kotlinx.coroutines.delay(1_500)
            refreshing.value = false
        }
    }

    fun search(q: String) = viewModelScope.launch {
        searchResults.value = if (q.isBlank()) emptyList() else repo.search(q)
    }

    fun add(hit: SearchHit) = viewModelScope.launch {
        repo.addToWatchlist(hit.symbol, hit.name, hit.exchangeShortName)
        searchResults.value = emptyList()
    }

    fun remove(ticker: String) = viewModelScope.launch { repo.removeFromWatchlist(ticker) }
}

private fun List<EtfWithQuote>.sortedBy(mode: SortMode): List<EtfWithQuote> = when (mode) {
    SortMode.Ticker -> sortedBy { it.etf.ticker }
    SortMode.Inverse -> filter { (it.etf.leverageFactor ?: 0) < 0 }.sortedBy { it.etf.ticker }
    SortMode.NonInverse -> filter { (it.etf.leverageFactor ?: 0) >= 0 }.sortedBy { it.etf.ticker }
    SortMode.Lev1x -> filter { kotlin.math.abs(it.etf.leverageFactor ?: 1) == 1 }.sortedBy { it.etf.ticker }
    SortMode.Lev2x -> filter { kotlin.math.abs(it.etf.leverageFactor ?: 0) == 2 }.sortedBy { it.etf.ticker }
    SortMode.Lev3x -> filter { kotlin.math.abs(it.etf.leverageFactor ?: 0) == 3 }.sortedBy { it.etf.ticker }
    SortMode.Price -> sortedByDescending { it.quote?.price ?: Double.NEGATIVE_INFINITY }
    SortMode.ChangePct -> sortedByDescending { it.quote?.changePct ?: Double.NEGATIVE_INFINITY }
    SortMode.Yield -> sortedByDescending { it.quote?.dividendYield ?: Double.NEGATIVE_INFINITY }
    SortMode.MonthReturn -> sortedByDescending { it.quote?.monthReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.TwoMonthReturn -> sortedByDescending { it.quote?.twoMonthReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.Week1Return -> sortedByDescending { it.quote?.week1ReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.Week2Return -> sortedByDescending { it.quote?.week2ReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.Week3Return -> sortedByDescending { it.quote?.week3ReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.Week5Return -> sortedByDescending { it.quote?.week5ReturnPct ?: Double.NEGATIVE_INFINITY }
    SortMode.Sector -> sortedBy { it.etf.sector ?: "zzz" }
}
