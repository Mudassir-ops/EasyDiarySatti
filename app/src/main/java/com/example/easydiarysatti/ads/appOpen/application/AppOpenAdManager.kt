package com.example.easydiarysatti.ads.appOpen.application

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date


// In AppOpenAdManager.kt
abstract class AppOpenAdManager {
    // 1. Change to a Map to store multiple ads simultaneously
    private val appOpenAdMap = mutableMapOf<String, AppOpenAd>()
    private val loadTimeMap = mutableMapOf<String, Long>()

    // 2. Use a Set to track which specific keys are currently loading
    private val loadingKeys = mutableSetOf<String>()

    fun loadAppOpen(
        context: Context,
        adType: String,
        appOpenId: String,
        adEnable: Boolean,
        isAppPurchased: Boolean,
        isInternetConnected: Boolean,
        listener: AppOpenOnLoadCallBack? = null
    ) {
        if (!adEnable || isAppPurchased || !isInternetConnected || appOpenId.isEmpty()) {
            listener?.onResponse(false)
            return
        }

        if (isAdAvailable(adType)) {
            listener?.onResponse(true)
            return
        }

        // 3. Only block if the SPECIFIC key is already loading
        if (loadingKeys.contains(adType)) return
        loadingKeys.add(adType)

        val request = AdRequest.Builder().build()
        AppOpenAd.load(context, appOpenId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAdMap[adType] = ad
                loadTimeMap[adType] = System.currentTimeMillis()
                loadingKeys.remove(adType) // Release this key
                listener?.onResponse(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loadingKeys.remove(adType)
                listener?.onResponse(false, error.message)
            }
        })
    }

    fun showAppOpen(
        activity: Activity?,
        adType: String,
        isAppPurchased: Boolean,
        listener: AppOpenOnShowCallBack? = null
    ) {
        val ad = appOpenAdMap[adType]

        if (isAppPurchased || ad == null || !isAdAvailable(adType)) {
            listener?.onAdFailedToShow()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAdMap.remove(adType) // 4. Only clear the ad that was shown
                listener?.onAdDismissedFullScreenContent()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAdMap.remove(adType)
                listener?.onAdFailedToShow()
            }
        }
        activity?.let { ad.show(it) }
    }

    private fun isAdAvailable(adType: String): Boolean {
        val ad = appOpenAdMap[adType]
        val loadTime = loadTimeMap[adType] ?: 0L
        return ad != null && (System.currentTimeMillis() - loadTime) < 4 * 60 * 60 * 1000
    }
}