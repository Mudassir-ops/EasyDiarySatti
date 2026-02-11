package com.example.easydiarysatti

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    @Inject
    lateinit var appOpenAdsConfig: AppOpenAdsConfig

    private var isInBackground = false
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)


        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            // We update currentActivity in OnStarted/OnResumed to ensure we have a valid context
            override fun onActivityStarted(activity: Activity) { currentActivity = activity }
            override fun onActivityResumed(activity: Activity) { currentActivity = activity }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity == activity) currentActivity = null
            }
        })
    }

    override fun onStop(owner: LifecycleOwner) {
        // Only set background state if we aren't bypassing (e.g., cropping or camera)
        if (sessionManagerRepo.isBypassSecurityLogin()) return
        isInBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isInBackground) {
            isInBackground = false
            showAdOnResume()
        }
    }
    private fun showAdOnResume() {
        if (sessionManagerRepo.isBypassSecurityLogin()) return

        // DO NOT change this call, just add this IF check at the top
        if (currentActivity?.javaClass?.simpleName == "SplashActivity") return

        currentActivity?.let { activity ->
            appOpenAdsConfig.showAppOpenAd(
                activity = activity,
                adType = AppOpenAdKey.RESUME, // Slot 1
                listener = object : AppOpenOnShowCallBack {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.RESUME)
                    }
                    override fun onAdFailedToShow() {
                        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.RESUME)
                    }
                }
            )
        }
    }
}
