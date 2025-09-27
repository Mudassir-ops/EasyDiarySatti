package com.example.easydiarysatti

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EasyDiaryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}