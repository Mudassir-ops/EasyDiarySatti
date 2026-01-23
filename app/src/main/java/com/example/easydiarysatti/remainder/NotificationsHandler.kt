package com.example.easydiarysatti.remainder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.MainActivity
import com.example.easydiarysatti.R
import com.example.easydiarysatti.REMAINDER_UNIQUE_ID

class NotificationsHandler(private val context: Context?) {

    fun createNotification(
        text: String?,
        uniqueId: Int = generateUniqueId(),
        contentTitle: String
    ) {
        if (context == null) return
        val channelId = "channel_reminders"

        // 1. Create the Intent that opens the app
        val intent = Intent(context, MainActivity::class.java).apply {
            // Flags to ensure the activity opens correctly
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Pass the uniqueId so MainActivity knows which note to open
            putExtra(REMAINDER_UNIQUE_ID, uniqueId)
        }

        // 2. Wrap it in a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueId, // Unique ID prevents intents from overwriting each other
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

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
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(contentTitle)
            .setContentText(text)
            .setStyle(bigTextStyle)
            .setDefaults(Notification.DEFAULT_SOUND)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure it pops up
            .setContentIntent(pendingIntent)              // CLICK ACTION
            .setAutoCancel(true)                           // REMOVE ON CLICK

        val notificationManagerCompat = NotificationManagerCompat.from(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManagerCompat.notify(uniqueId, notificationBuilder.build())
        }
    }

    private fun generateUniqueId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

}
