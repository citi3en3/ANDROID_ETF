package com.iurie.etfwatch.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.remote.SearchHit
import com.iurie.etfwatch.data.repo.AddResult
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.ui.common.EtfSorting
import com.iurie.etfwatch.ui.common.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val items: List<EtfWithQuote> = emptyList(),
    val sort: SortMode = SortMode.Ticker,
    val isRefreshing: Boolean = false,
    val searchResults: List<SearchHit> = emptyList(),
    val isSearching: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repo: EtfRepository,
) : ViewModel() {

    private val sort = MutableStateFlow(SortMode.Ticker)
    private val refreshing = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val searching = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /**
     * Debounced and latest-wins. Firing one request per keystroke meant a slow response for "H"
     * could land after the results for "HDIV" and overwrite them; flatMapLatest cancels the
     * superseded call instead.
     */
    private val searchResults: StateFlow<List<SearchHit>> = query
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                searching.value = false
                flowOf(emptyList())
            } else {
                flow {
                    searching.value = true
                    try {
                        emit(repo.search(q))
                    } finally {
                        searching.value = false
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<WatchlistUiState> =
        combine(repo.watchlist(), sort, refreshing, searchResults, searching) { items, s, r, sr, searchInFlight ->
            WatchlistUiState(
                items = EtfSorting.sort(items, s),
                sort = s,
                isRefreshing = r,
                searchResults = sr,
                isSearching = searchInFlight,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

    init {
        viewModelScope.launch { runCatching { repo.ensureSeeded() } }
    }

    fun setSort(s: SortMode) { sort.value = s }

    /**
     * Runs the refresh directly and reports real progress. It used to enqueue a worker and flip
     * the spinner off after a fixed 1.5s, which was unrelated to whether the work had finished.
     */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repo.refreshAll() }
                .onFailure { _messages.tryEmit("Refresh failed — check your connection") }
            refreshing.value = false
        }
    }

    fun search(q: String) { query.value = q }

    fun add(hit: SearchHit) = viewModelScope.launch {
        when (val result = repo.addToWatchlist(hit.symbol, hit.name, hit.exchangeShortName)) {
            is AddResult.Added -> _messages.tryEmit("Added ${result.ticker} to watchlist")
            is AddResult.Rejected -> _messages.tryEmit(result.reason)
        }
        query.value = ""
    }

    fun remove(ticker: String) = viewModelScope.launch {
        repo.removeFromWatchlist(ticker)
        _messages.tryEmit("Removed $ticker from watchlist")
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
