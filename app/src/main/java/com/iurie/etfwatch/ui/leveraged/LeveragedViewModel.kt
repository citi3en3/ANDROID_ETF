package com.iurie.etfwatch.ui.leveraged

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

data class LeveragedUiState(
    val grouped: Map<String, List<EtfWithQuote>> = emptyMap(),
    val flat: List<EtfWithQuote> = emptyList(),
    val sort: SortMode = SortMode.Sector,
    val filters: Set<SortMode> = setOf(
        SortMode.Inverse, SortMode.NonInverse,
        SortMode.Lev1x, SortMode.Lev2x, SortMode.Lev3x,
    ),
    val isRefreshing: Boolean = false,
)

private val DIRECTION_FILTERS = setOf(SortMode.Inverse, SortMode.NonInverse)
private val MAGNITUDE_FILTERS = setOf(SortMode.Lev1x, SortMode.Lev2x, SortMode.Lev3x)
private val DEFAULT_LEV_FILTERS = DIRECTION_FILTERS + MAGNITUDE_FILTERS

@HiltViewModel
class LeveragedViewModel @Inject constructor(
    private val repo: EtfRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sort = MutableStateFlow(SortMode.Sector)
    private val filters = MutableStateFlow(DEFAULT_LEV_FILTERS)

    val state: StateFlow<LeveragedUiState> =
        combine(repo.leveraged(), sort, filters, refreshing) { list, s, f, r ->
            val includeInverse = SortMode.Inverse in f
            val includeNonInverse = SortMode.NonInverse in f
            val allowedMagnitudes = buildSet {
                if (SortMode.Lev1x in f) add(1)
                if (SortMode.Lev2x in f) add(2)
                if (SortMode.Lev3x in f) add(3)
            }
            val byDirection = when {
                includeInverse && includeNonInverse -> list
                includeInverse -> list.filter { (it.etf.leverageFactor ?: 0) < 0 }
                includeNonInverse -> list.filter { (it.etf.leverageFactor ?: 0) >= 0 }
                else -> emptyList()
            }
            val filtered = if (allowedMagnitudes.isEmpty()) emptyList() else byDirection.filter {
                val mag = kotlin.math.abs(it.etf.leverageFactor ?: 1)
                mag in allowedMagnitudes
            }

            val sortedFlat = when (s) {
                SortMode.MonthReturn -> filtered.sortedByDescending { it.quote?.monthReturnPct ?: Double.NEGATIVE_INFINITY }
                SortMode.Ticker -> filtered.sortedBy { it.etf.ticker }
                SortMode.Price -> filtered.sortedByDescending { it.quote?.price ?: Double.NEGATIVE_INFINITY }
                SortMode.ChangePct -> filtered.sortedByDescending { it.quote?.changePct ?: Double.NEGATIVE_INFINITY }
                SortMode.Yield -> filtered.sortedByDescending { it.quote?.dividendYield ?: Double.NEGATIVE_INFINITY }
                SortMode.Sector -> filtered
                SortMode.Inverse,
                SortMode.NonInverse,
                SortMode.Lev1x,
                SortMode.Lev2x,
                SortMode.Lev3x -> filtered.sortedBy { it.etf.ticker }
            }
            val grouped = if (s == SortMode.Sector) sortedFlat.groupBy { it.etf.sector ?: "Other" } else emptyMap()
            LeveragedUiState(grouped = grouped, flat = sortedFlat, sort = s, filters = f, isRefreshing = r)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeveragedUiState())

    fun setSort(mode: SortMode) { sort.value = mode }

    fun toggleFilter(mode: SortMode) {
        if (mode !in DIRECTION_FILTERS && mode !in MAGNITUDE_FILTERS) return
        filters.value = filters.value.toMutableSet().apply {
            if (contains(mode)) remove(mode) else add(mode)
        }
    }

    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repo.refreshAll() }
            scheduler.runOnceNow()
            refreshing.value = false
        }
    }
}
