package com.example.easydiarysatti.source

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class FirebaseAnalyticsDataSource @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    fun logEvent(name: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }
}