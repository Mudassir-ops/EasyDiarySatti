package com.example.easydiarysatti.remainder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.easydiarysatti.REMAINDER_INTENT
import com.example.easydiarysatti.REMAINDER_UNIQUE_ID

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val text = intent.getStringExtra(REMAINDER_INTENT)
        val uniqueId =
            intent.getIntExtra(
                REMAINDER_UNIQUE_ID,
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            )
        val notificationsHandler = NotificationsHandler(context)
        notificationsHandler.createNotification(text, uniqueId)
    }

}