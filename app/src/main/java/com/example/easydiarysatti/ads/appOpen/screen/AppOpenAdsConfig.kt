package com.example.easydiarysatti.ads.appOpen.screen

import android.app.Activity
import android.content.Context
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.appOpen.screen.manager.AppOpenManager
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils

class AppOpenAdsConfig(
    private val context: Context,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : AppOpenManager() {

    fun loadAppOpenAd(adType: AppOpenAdKey, listener: AppOpenOnLoadCallBack? = null) {
        var interAdId = ""
        var isRemoteEnable = false

        when (adType) {
            AppOpenAdKey.THEME -> {
                interAdId = context.getString(R.string.admob_app_open_id)
                isRemoteEnable = sharedPreferenceUtils.rcAppOpen != 0
            }
        }

        loadAppOpen(
            context = context,
            adType = adType.value,
            appOpenId = interAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showAppOpenAd(
        activity: Activity?,
        adType: AppOpenAdKey,
        listener: AppOpenOnShowCallBack? = null
    ) {
        showAppOpen(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener
        )
    }
}