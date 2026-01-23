package com.example.easydiarysatti.remainder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.easydiarysatti.CONTENT_TITLE
import com.example.easydiarysatti.R
import com.example.easydiarysatti.REMAINDER_INTENT
import com.example.easydiarysatti.REMAINDER_UNIQUE_ID

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("AlarmReceiver", "Alarm received: ${intent?.action}")

        if (context == null || intent == null) {
            Log.e("AlarmReceiver", "Context or Intent is null")
            return
        }

        val text = intent.getStringExtra(REMAINDER_INTENT)
        val contentTitle = intent.getStringExtra(CONTENT_TITLE)
            ?: context.getString(R.string.reminder)
        val uniqueId = intent.getIntExtra(
            REMAINDER_UNIQUE_ID,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        )

        Log.d("AlarmReceiver", "Creating notification for ID: $uniqueId, Title: $contentTitle, Text: $text")

        val notificationsHandler = NotificationsHandler(context)
        notificationsHandler.createNotification(
            text = text,
            uniqueId = uniqueId,
            contentTitle = contentTitle
        )
    }
}