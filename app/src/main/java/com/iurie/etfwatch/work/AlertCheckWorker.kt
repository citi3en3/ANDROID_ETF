package com.iurie.etfwatch.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iurie.etfwatch.data.repo.AlertRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @ApplicationContext private val ctx: Context,
    private val alertRepo: AlertRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        ensureChannel()
        val triggered = alertRepo.checkAll()
        triggered.forEach { t -> notify(t.alert.id.toInt(), t.alert.ticker, t.alert.direction, t.alert.threshold, t.price) }
        Result.success()
    }.getOrElse {
        Timber.w(it, "AlertCheckWorker failed")
        Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Price Alerts", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    private fun notify(id: Int, ticker: String, direction: String, threshold: Double, price: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("etfwatch://detail/$ticker"))
        val pi = PendingIntent.getActivity(ctx, id, deepLink, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("$ticker — alert")
            .setContentText("Price %.2f is $direction threshold %.2f".format(price, threshold))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    companion object {
        const val CHANNEL = "price_alerts"
    }
}
