package com.example.easydiarysatti.ads.appOpen.application

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(
    private val context: Context,
    private val internetManager: InternetManager,
    private val sharedPrefs: SharedPreferenceUtils
) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime = 0L

    private val appOpenId by lazy { context.getString(R.string.admob_app_open_id) }

    fun loadAppOpen() {
        if (isLoadingAd || isAdAvailable()) return

        if (sharedPrefs.isAppPurchased) return
        if (!internetManager.isInternetConnected) return
        if (appOpenId.isBlank()) return

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            context,
            appOpenId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d("AppOpenAd", "Ad loaded")
                    appOpenAd = ad
                    loadTime = Date().time
                    isLoadingAd = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AppOpenAd", "Ad failed to load: ${error.message}")
                    isLoadingAd = false
                }
            }
        )
    }

    fun showAppOpen(activity: Activity, onDismiss: () -> Unit) {
        if (!isAdAvailable()) {
            Log.d("AppOpenAd", "Ad not ready, reloading...")
            loadAppOpen()
            onDismiss()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("AppOpenAd", "Ad dismissed")
                appOpenAd = null
                onDismiss()
                loadAppOpen()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AppOpenAd", "Failed to show: ${adError.message}")
                onDismiss()
                loadAppOpen()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AppOpenAd", "Ad shown successfully")
            }
        }

        appOpenAd?.show(activity)
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && !wasAdExpired()
    }

    private fun wasAdExpired(): Boolean {
        val now = Date().time
        val diff = now - loadTime
        val expired = diff > 4 * 60 * 60 * 1000 // 4 hours
        if (expired) appOpenAd = null
        return expired
    }
}
