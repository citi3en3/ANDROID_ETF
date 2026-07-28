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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iurie.etfwatch.data.repo.TriggeredAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Posts price-alert notifications. Split out of the worker so it can be reused and reasoned about. */
@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    fun notifyAll(triggered: List<TriggeredAlert>) {
        if (triggered.isEmpty()) return
        if (!canPostNotifications()) return
        ensureChannel()
        triggered.forEach { notify(it) }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Price Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private fun notify(t: TriggeredAlert) {
        val id = t.alert.id.toInt()
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("etfwatch://detail/${t.alert.ticker}"))
        val pi = PendingIntent.getActivity(
            ctx,
            id,
            deepLink,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = String.format(
            Locale.US,
            "Price %.2f is %s threshold %.2f",
            t.price,
            t.alert.direction,
            t.alert.threshold,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("${t.alert.ticker} — alert")
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        // The grant can be revoked between the check above and this call; a background refresh
        // must not crash because the user turned notifications off a moment ago.
        try {
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission revoked; dropping alert for ${t.alert.ticker}")
        }
    }

    companion object {
        const val CHANNEL = "price_alerts"
    }
}
