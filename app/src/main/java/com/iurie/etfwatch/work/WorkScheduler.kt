package com.iurie.etfwatch.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.iurie.etfwatch.data.prefs.UserPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val prefs: UserPrefs,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(ctx)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    suspend fun schedulePeriodicRefreshFromPrefs() {
        schedulePeriodicRefresh(prefs.refreshIntervalMinutes.first().toLong())
    }

    fun schedulePeriodicRefresh(intervalMinutes: Long = UserPrefs.DEFAULT_INTERVAL_MIN.toLong()) {
        val refresh = PeriodicWorkRequestBuilder<QuoteRefreshWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.UPDATE, refresh)

        // Alerts used to run as their own periodic job; they are part of the refresh now, so
        // retire the old one on installs that still have it enqueued.
        workManager.cancelUniqueWork(LEGACY_ALERT_WORK)
    }

    fun runOnceNow() {
        workManager.enqueueUniqueWork(
            ONE_SHOT_WORK,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<QuoteRefreshWorker>().setConstraints(networkConstraints).build(),
        )
    }

    private companion object {
        const val REFRESH_WORK = "quote_refresh"
        const val ONE_SHOT_WORK = "quote_refresh_once"
        const val LEGACY_ALERT_WORK = "alert_check"
    }
}
