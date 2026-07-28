package com.iurie.etfwatch.ui.leveraged

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.ui.common.EtfSorting
import com.iurie.etfwatch.ui.common.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    val filters: Set<SortMode> = DEFAULT_LEV_FILTERS,
    val isRefreshing: Boolean = false,
)

private val DIRECTION_FILTERS = setOf(SortMode.Inverse, SortMode.NonInverse)
private val MAGNITUDE_FILTERS = setOf(SortMode.Lev1x, SortMode.Lev2x, SortMode.Lev3x)
internal val DEFAULT_LEV_FILTERS = DIRECTION_FILTERS + MAGNITUDE_FILTERS
private const val UNCLASSIFIED = "Other"

@HiltViewModel
class LeveragedViewModel @Inject constructor(
    private val repo: EtfRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sort = MutableStateFlow(SortMode.Sector)
    private val filters = MutableStateFlow(DEFAULT_LEV_FILTERS)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<LeveragedUiState> =
        combine(repo.leveraged(), sort, filters, refreshing) { list, s, f, r ->
            val filtered = applyFilters(list, f)
            val sortedFlat = EtfSorting.sort(filtered, s)
            LeveragedUiState(
                grouped = if (s == SortMode.Sector) sortedFlat.groupBy { it.etf.sector ?: UNCLASSIFIED } else emptyMap(),
                flat = sortedFlat,
                sort = s,
                filters = f,
                isRefreshing = r,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeveragedUiState())

    fun setSort(mode: SortMode) { sort.value = mode }

    fun toggleFilter(mode: SortMode) {
        if (mode !in DIRECTION_FILTERS && mode !in MAGNITUDE_FILTERS) return
        filters.value = filters.value.toMutableSet().apply {
            if (contains(mode)) remove(mode) else add(mode)
        }
    }

    /** One refresh per pull; this used to run the repository call and a duplicate worker job. */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repo.refreshAll() }
                .onFailure { _messages.tryEmit("Refresh failed — check your connection") }
            refreshing.value = false
        }
    }
}

/**
 * Direction and magnitude are independent filters: a fund must pass both. A fund with no declared
 * leverage factor has unknown magnitude and is excluded from magnitude filtering rather than being
 * silently counted as 1x.
 */
internal fun applyFilters(items: List<EtfWithQuote>, filters: Set<SortMode>): List<EtfWithQuote> {
    val includeInverse = SortMode.Inverse in filters
    val includeNonInverse = SortMode.NonInverse in filters
    val allowedMagnitudes = buildSet {
        if (SortMode.Lev1x in filters) add(1)
        if (SortMode.Lev2x in filters) add(2)
        if (SortMode.Lev3x in filters) add(3)
    }
    if (allowedMagnitudes.isEmpty()) return emptyList()

    return items.filter { item ->
        val directionOk = if (EtfSorting.isInverse(item)) includeInverse else includeNonInverse
        directionOk && EtfSorting.magnitude(item) in allowedMagnitudes
    }
}
