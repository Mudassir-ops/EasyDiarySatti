package com.example.easydiarysatti.remainder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.easydiarysatti.CONTENT_TITLE
import com.example.easydiarysatti.REMAINDER_INTENT
import com.example.easydiarysatti.REMAINDER_UNIQUE_ID
import java.util.Calendar


class AlarmHandler(private val context: Context?) {

    fun createAlarm(c: Calendar, text: String?, uniqueId: Int, contentTitle: String) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context ?: return, AlarmReceiver::class.java).apply {
            action = "${context.packageName}.alarm.$uniqueId"
            putExtra(REMAINDER_INTENT, text)
            putExtra(REMAINDER_UNIQUE_ID, uniqueId)
            putExtra(CONTENT_TITLE, contentTitle)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager?.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            c.timeInMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(uniqueId: Int) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "${context?.packageName}.alarm.$uniqueId"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        pendingIntent.cancel()
        alarmManager?.cancel(pendingIntent)
    }
}
