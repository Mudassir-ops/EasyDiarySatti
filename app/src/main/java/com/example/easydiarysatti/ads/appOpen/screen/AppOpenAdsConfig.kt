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
import jakarta.inject.Inject

//class AppOpenAdsConfig(
//    private val context: Context,
//    private val sharedPreferenceUtils: SharedPreferenceUtils,
//    private val internetManager: InternetManager
//) : AppOpenManager() {
//
//    fun loadAppOpenAd(adType: AppOpenAdKey, listener: AppOpenOnLoadCallBack? = null) {
//        var interAdId = ""
//        var isRemoteEnable = false
//
//        when (adType) {
//            AppOpenAdKey.THEME -> {
//                interAdId = context.getString(R.string.admob_app_open_id)
//                isRemoteEnable = sharedPreferenceUtils.rcAppOpen != 0
//            }
//
//            AppOpenAdKey.NAME_SCREEN ->{
//                interAdId = context.getString(R.string.admob_app_open_id)
//                isRemoteEnable = sharedPreferenceUtils.rcAppOpen != 0
//            }
//        }
//
//        loadAppOpen(
//            context = context,
//            adType = adType.value,
//            appOpenId = interAdId,
//            adEnable = isRemoteEnable,
//            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
//            isInternetConnected = internetManager.isInternetConnected,
//            listener = listener
//        )
//    }
//
//    fun showAppOpenAd(
//        activity: Activity?,
//        adType: AppOpenAdKey,
//        listener: AppOpenOnShowCallBack? = null
//    ) {
//        showAppOpen(
//            activity = activity,
//            adType = adType.value,
//            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
//            listener
//        )
//    }
//}

class AppOpenAdsConfig @Inject constructor(
    private val context: Context,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : AppOpenManager() {

    fun loadAppOpenAd(adType: AppOpenAdKey, listener: AppOpenOnLoadCallBack? = null) {
        // Fetch ID and Show Status directly from JSON
        val adId = sharedPreferenceUtils.getAdId(adType.value)
        val isRemoteEnable = sharedPreferenceUtils.getAdShowStatus(adType.value)

        loadAppOpen(
            context = context,
            adType = adType.value,
            appOpenId = adId,
            adEnable = isRemoteEnable && adId.isNotEmpty(),
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
        // ── Frequency gate for the RESUME slot ───────────────────────────────
        // Applies the Remote Config rule (off / always / 24h / session-N) before
        // attempting to display the ad.  Other slots (SPLASH_FIRST_TIME,
        // SPLASH_RETURN_USER) are not affected — they control their own logic
        // in the Splash screen.
        if (adType == AppOpenAdKey.RESUME) {
            if (!sharedPreferenceUtils.shouldShowAppOpenOnResume()) {
                // Frequency rule says "not yet" — fire the fail callback so the
                // caller (EasyDiaryApplication) still reloads the ad for next time.
                listener?.onAdFailedToShow()
                return
            }
        }

        showAppOpen(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener = listener
        )
    }
}