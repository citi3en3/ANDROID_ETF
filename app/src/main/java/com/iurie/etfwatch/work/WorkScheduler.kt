package com.iurie.etfwatch.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun schedulePeriodicRefresh(intervalMinutes: Long = 30L) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refresh = PeriodicWorkRequestBuilder<QuoteRefreshWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "quote_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            refresh,
        )

        val alerts = PeriodicWorkRequestBuilder<AlertCheckWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "alert_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            alerts,
        )
    }

    fun runOnceNow() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            "quote_refresh_once",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<QuoteRefreshWorker>().setConstraints(constraints).build(),
        )
    }
}
