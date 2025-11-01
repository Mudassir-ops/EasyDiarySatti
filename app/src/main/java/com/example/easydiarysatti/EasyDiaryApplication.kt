package com.example.easydiarysatti

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp

class MyApp : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    private var isInBackground = false

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // ✅ Step 1: Initialize AdMob FIRST
        MobileAds.initialize(this) { status ->
            Log.d("MyApp", "AdMob initialized successfully: $status")

            // ✅ Step 2: Set test device IDs safely
//            val config = RequestConfiguration.Builder()
//                .setTestDeviceIds(listOf("E13FEE4C2083A31575BFEFD22146CE76"))
//                .build()
//            MobileAds.setRequestConfiguration(config)

            // ✅ Step 3: Now attach lifecycle observer
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }

        // to get test ads on this device."
//        val testDeviceId = AdRequest.Builder().build().requestAgent
//        Log.d("AdMobSatti", "Test Device ID: $testDeviceId")
        //  MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(listOf("E13FEE4C2083A31575BFEFD22146CE76")).build())
    }

    override fun onStop(owner: LifecycleOwner) {
        if (sessionManagerRepo.isBypassSecurityLogin()) return
        isInBackground = true
        sessionManagerRepo.setRequireLogin(true)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isInBackground) {
            isInBackground = false
        }
    }
}

