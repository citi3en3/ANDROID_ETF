package com.iurie.etfwatch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.data.repo.QuoteRepository
import com.iurie.etfwatch.ui.common.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * No portfolio totals here on purpose. The app stores no share quantities, so any "total value"
 * would be the sum of one share of each watchlist entry — mixing USD and CAD prices — and the
 * "total change" an unweighted average percentage applied to it. Both looked authoritative and
 * were meaningless, so they are gone rather than merely relabelled.
 */
data class HomeUiState(
    val watchlistItems: List<EtfWithQuote> = emptyList(),
    val topEtfs: List<EtfWithQuote> = emptyList(),
    val sparklines: Map<String, List<ChartPoint>> = emptyMap(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val etfRepo: EtfRepository,
    private val quoteRepo: QuoteRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sparklines = MutableStateFlow<Map<String, List<ChartPoint>>>(emptyMap())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<HomeUiState> = combine(
        etfRepo.watchlist(),
        etfRepo.leveraged(),
        sparklines,
        refreshing,
    ) { watchlist, leveraged, sparks, isRefreshing ->
        HomeUiState(
            watchlistItems = watchlist,
            topEtfs = leveraged.take(TOP_ETF_COUNT),
            sparklines = sparks,
            isRefreshing = isRefreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { fetchSparklines() }
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { etfRepo.refreshAll() }
                .onFailure { _messages.tryEmit("Refresh failed — check your connection") }
            fetchSparklines()
            refreshing.value = false
        }
    }

    /** Sparkline history is one call per ticker, so fetch them concurrently and cap the fan-out. */
    private suspend fun fetchSparklines() {
        runCatching {
            val leveraged = etfRepo.leveraged().first().take(TOP_ETF_COUNT)
            val watchlist = etfRepo.watchlist().first()
            val tickers = (leveraged + watchlist)
                .map { it.etf.ticker }
                .distinct()
                .take(MAX_SPARKLINES)
            sparklines.value = quoteRepo.histories(tickers, SPARKLINE_DAYS)
        }.onFailure { Timber.w(it, "Sparkline fetch failed") }
    }

    private companion object {
        const val TOP_ETF_COUNT = 5
        const val SPARKLINE_DAYS = 30

        /** Upper bound on per-ticker history calls made just to draw thumbnails. */
        const val MAX_SPARKLINES = 20
    }
}
