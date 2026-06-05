package org.mediadownloader.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val CHANNEL_ID = "downloads"
        const val CHANNEL_NAME = "Downloads"
    }

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }

    fun buildProgress(progressPercent: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading video…")
            .setProgress(100, progressPercent, progressPercent == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun buildSuccess(notificationId: Int, filePath: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(filePath.toUri(), "video/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText("Tap to open")
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
    }

    fun buildFailure(reason: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()

    fun show(notificationId: Int, notification: Notification) {
        manager.notify(notificationId, notification)
    }
}
