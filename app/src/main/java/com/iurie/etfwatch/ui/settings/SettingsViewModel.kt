package com.iurie.etfwatch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.prefs.UserPrefs
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val intervalMinutes: Int = UserPrefs.DEFAULT_INTERVAL_MIN,
    val lastRefreshAt: Long = 0L,
    val choices: List<Int> = UserPrefs.INTERVAL_CHOICES,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: EtfRepository,
    private val scheduler: WorkScheduler,
    private val prefs: UserPrefs,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(prefs.refreshIntervalMinutes, prefs.lastRefreshAt) { i, ts ->
            SettingsUiState(intervalMinutes = i, lastRefreshAt = ts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshNow() = scheduler.runOnceNow()

    fun reseed() = viewModelScope.launch { repo.ensureSeeded() }

    fun setInterval(minutes: Int) = viewModelScope.launch {
        prefs.setRefreshIntervalMinutes(minutes)
        scheduler.schedulePeriodicRefresh(minutes.toLong())
    }
}

