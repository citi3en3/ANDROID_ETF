package com.iurie.etfwatch.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.ui.common.ChartPoint
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.data.repo.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalValue: Double = 0.0,
    val totalChangePct: Double = 0.0,
    val totalChangeDollar: Double = 0.0,
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

    val state: StateFlow<HomeUiState> = combine(
        etfRepo.watchlist(),
        etfRepo.leveraged(),
        sparklines,
        refreshing,
    ) { watchlist, leveraged, sparks, r ->
        val topEtfs = leveraged.take(5)
        val totalValue = watchlist.sumOf { it.quote?.price ?: 0.0 }
        val changeItems = watchlist.filter { it.quote?.changePct != null }
        val avgChangePct =
            if (changeItems.isEmpty()) 0.0
            else changeItems.sumOf { it.quote!!.changePct!! } / changeItems.size
        val totalChangeDollar = totalValue * avgChangePct / 100.0
        HomeUiState(
            totalValue = totalValue,
            totalChangePct = avgChangePct,
            totalChangeDollar = totalChangeDollar,
            watchlistItems = watchlist,
            topEtfs = topEtfs,
            sparklines = sparks,
            isRefreshing = r,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { fetchSparklines() }
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try { etfRepo.refreshAll() } catch (_: Exception) {}
            fetchSparklines()
            refreshing.value = false
        }
    }

    private suspend fun fetchSparklines() {
        try {
            val leveraged = etfRepo.leveraged().first()
            val watchlist = etfRepo.watchlist().first()
            
            // Fetch for top 5 leveraged AND all watchlist items
            val tickersToFetch = (leveraged.take(5) + watchlist).map { it.etf.ticker }.distinct()
            
            val result = mutableMapOf<String, List<ChartPoint>>()
            tickersToFetch.forEach { ticker ->
                try {
                    val pts = quoteRepo.history(ticker, 30)
                    if (pts.isNotEmpty()) result[ticker] = pts
                } catch (_: Exception) {}
            }
            sparklines.value = result
        } catch (_: Exception) {}
    }
}
