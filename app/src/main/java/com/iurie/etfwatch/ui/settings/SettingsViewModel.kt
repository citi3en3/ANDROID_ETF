package com.iurie.etfwatch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: EtfRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {
    fun refreshNow() = scheduler.runOnceNow()
    fun reseed() = viewModelScope.launch { repo.ensureSeeded() }
}
