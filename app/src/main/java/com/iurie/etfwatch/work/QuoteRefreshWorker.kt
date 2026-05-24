package com.iurie.etfwatch.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iurie.etfwatch.data.prefs.UserPrefs
import com.iurie.etfwatch.data.repo.EtfRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class QuoteRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val etfRepo: EtfRepository,
    private val prefs: UserPrefs,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        etfRepo.refreshAll()
        prefs.markRefreshed()
        Result.success()
    }.getOrElse {
        Timber.w(it, "QuoteRefreshWorker failed")
        Result.retry()
    }
}
