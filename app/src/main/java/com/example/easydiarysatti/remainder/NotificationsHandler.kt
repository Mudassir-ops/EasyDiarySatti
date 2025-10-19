package com.example.easydiarysatti.remainder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.easydiarysatti.R

class NotificationsHandler(private val context: Context?) {

    fun createNotification(
        text: String?,
        uniqueId: Int = generateUniqueId(),
        contentTitle: String
    ) {
        if (context == null) return
        val channelId = "channel_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelName: CharSequence = "Reminders channel"
            val channelDescription = "Notification channel for the reminders"
            val channelImportance = NotificationManager.IMPORTANCE_HIGH

            val notificationChannel =
                NotificationChannel(channelId, channelName, channelImportance).apply {
                    description = channelDescription
                }
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle().bigText(text)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_icon_svg)
            .setContentTitle(contentTitle)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setAutoCancel(true)

        val notificationManagerCompat = NotificationManagerCompat.from(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        notificationManagerCompat.notify(uniqueId, notificationBuilder.build())
    }

    private fun generateUniqueId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }
}
