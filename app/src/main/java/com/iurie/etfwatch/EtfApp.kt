package com.iurie.etfwatch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.iurie.etfwatch.data.prefs.UserPrefs
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class EtfApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var etfRepository: EtfRepository
    @Inject lateinit var userPrefs: UserPrefs

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        appScope.launch {
            runCatching { etfRepository.ensureSeeded() }
                .onFailure { Timber.e(it, "Seed failed") }
            workScheduler.schedulePeriodicRefreshFromPrefs()
            if (isDataStale()) workScheduler.runOnceNow()
        }
    }

    /**
     * A full refresh costs one FMP call per tracked ticker, so firing one on every cold start
     * burns the call budget for nothing when the data is already current.
     */
    private suspend fun isDataStale(): Boolean = runCatching {
        val last = userPrefs.lastRefreshAt.first()
        val intervalMs = TimeUnit.MINUTES.toMillis(userPrefs.refreshIntervalMinutes.first().toLong())
        System.currentTimeMillis() - last >= intervalMs
    }.getOrDefault(true)
}
