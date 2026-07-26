package com.iurie.etfwatch.ui.hamilton

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.ui.common.SortMode
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HamiltonUiState(
    val grouped: Map<String, List<EtfWithQuote>> = emptyMap(),
    val flat: List<EtfWithQuote> = emptyList(),
    val sort: SortMode = SortMode.Sector,
    val availableSectors: List<String> = emptyList(),
    val sectorFilters: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class HamiltonViewModel @Inject constructor(
    private val repo: EtfRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sort = MutableStateFlow(SortMode.Sector)
    private val sectorFilters = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<HamiltonUiState> =
        combine(repo.hamilton(), sort, sectorFilters, refreshing) { items, s, sf, r ->
            val sectors = items.map { it.etf.sector ?: "Other" }.distinct().sorted()
            val effectiveFilter = if (sf.isEmpty()) sectors.toSet() else sf
            val filtered = items.filter { (it.etf.sector ?: "Other") in effectiveFilter }
            val sortedFlat = when (s) {
                SortMode.MonthReturn -> filtered.sortedByDescending { it.quote?.monthReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.TwoMonthReturn -> filtered.sortedByDescending { it.quote?.twoMonthReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Week1Return -> filtered.sortedByDescending { it.quote?.week1ReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Week2Return -> filtered.sortedByDescending { it.quote?.week2ReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Week3Return -> filtered.sortedByDescending { it.quote?.week3ReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Week5Return -> filtered.sortedByDescending { it.quote?.week5ReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Ticker -> filtered.sortedBy { it.etf.ticker }
                SortMode.Price -> filtered.sortedByDescending { it.quote?.price ?: Double.NEGATIVE_INFINITY }
                SortMode.ChangePct -> filtered.sortedByDescending { it.quote?.changePct ?: Double.NEGATIVE_INFINITY }
                SortMode.Yield -> filtered.sortedByDescending { it.quote?.dividendYield ?: Double.NEGATIVE_INFINITY }
                SortMode.Sector -> filtered
                else -> filtered.sortedBy { it.etf.ticker }
            }
            val grouped = if (s == SortMode.Sector) sortedFlat.groupBy { it.etf.sector ?: "Other" } else emptyMap()
            HamiltonUiState(
                grouped = grouped,
                flat = sortedFlat,
                sort = s,
                availableSectors = sectors,
                sectorFilters = sf,
                isRefreshing = r,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HamiltonUiState())

    fun setSort(mode: SortMode) { sort.value = mode }

    fun toggleSector(sector: String) {
        sectorFilters.value = sectorFilters.value.toMutableSet().apply {
            if (contains(sector)) remove(sector) else add(sector)
        }
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repo.refreshHamilton() }
            scheduler.runOnceNow()
            refreshing.value = false
        }
    }
}
