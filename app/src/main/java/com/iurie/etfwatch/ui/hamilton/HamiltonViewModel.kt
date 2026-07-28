package com.iurie.etfwatch.ui.hamilton

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

data class HamiltonUiState(
    val grouped: Map<String, List<EtfWithQuote>> = emptyMap(),
    val flat: List<EtfWithQuote> = emptyList(),
    val sort: SortMode = SortMode.Sector,
    val availableSectors: List<String> = emptyList(),
    val sectorFilters: Set<String> = emptySet(),
    val isRefreshing: Boolean = false,
)

private const val UNCLASSIFIED = "Other"

@HiltViewModel
class HamiltonViewModel @Inject constructor(
    private val repo: EtfRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val sort = MutableStateFlow(SortMode.Sector)
    private val sectorFilters = MutableStateFlow<Set<String>>(emptySet())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<HamiltonUiState> =
        combine(repo.hamilton(), sort, sectorFilters, refreshing) { items, s, sf, r ->
            val sectors = items.map { it.etf.sector ?: UNCLASSIFIED }.distinct().sorted()
            val effectiveFilter = if (sf.isEmpty()) sectors.toSet() else sf
            val filtered = items.filter { (it.etf.sector ?: UNCLASSIFIED) in effectiveFilter }
            val sortedFlat = EtfSorting.sort(filtered, s)
            HamiltonUiState(
                grouped = if (s == SortMode.Sector) sortedFlat.groupBy { it.etf.sector ?: UNCLASSIFIED } else emptyMap(),
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

    /**
     * Runs the refresh once. It used to call the repository *and* enqueue a worker that ran the
     * same refresh again — double the FMP calls, and two interleaved passes writing the same rows.
     */
    fun refresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            runCatching { repo.refreshHamilton() }
                .onFailure { _messages.tryEmit("Refresh failed — check your connection") }
            refreshing.value = false
        }
    }
}
