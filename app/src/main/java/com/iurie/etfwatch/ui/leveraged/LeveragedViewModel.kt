package com.iurie.etfwatch.ui.leveraged

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeveragedUiState(
    val grouped: Map<String, List<EtfWithQuote>> = emptyMap(),
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class LeveragedViewModel @Inject constructor(
    repo: EtfRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<LeveragedUiState> =
        combine(
            repo.leveraged().map { list -> list.groupBy { it.etf.sector ?: "Other" } },
            refreshing,
        ) { grouped, r -> LeveragedUiState(grouped = grouped, isRefreshing = r) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeveragedUiState())

    fun refresh() {
        refreshing.value = true
        scheduler.runOnceNow()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1_500)
            refreshing.value = false
        }
    }
}
