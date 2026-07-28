package com.iurie.etfwatch.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iurie.etfwatch.data.prefs.UserPrefs
import com.iurie.etfwatch.data.repo.AlertRepository
import com.iurie.etfwatch.data.repo.EtfRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Refreshes quotes, then evaluates price alerts against what it just wrote.
 *
 * Alerts run here rather than in their own periodic job because a separate job had no ordering
 * guarantee against this one, so it routinely judged thresholds on stale prices.
 */
@HiltWorker
class QuoteRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val etfRepo: EtfRepository,
    private val alertRepo: AlertRepository,
    private val notifier: AlertNotifier,
    private val prefs: UserPrefs,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        etfRepo.refreshAll()
        prefs.markRefreshed()
        runCatching { notifier.notifyAll(alertRepo.checkAll()) }
            .onFailure { Timber.w(it, "Alert check failed") }
        Result.success()
    }.getOrElse {
        Timber.w(it, "QuoteRefreshWorker failed")
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
