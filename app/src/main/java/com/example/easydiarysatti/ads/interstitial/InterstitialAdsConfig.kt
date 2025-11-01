package com.example.easydiarysatti.ads.interstitial

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnLoadCallBack
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.interstitial.enums.InterAdKey
import com.example.easydiarysatti.ads.interstitial.manager.InterstitialManager
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import javax.inject.Inject

/**
 * @param context: Can be of application class
 */
class InterstitialAdsConfig @Inject constructor(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : InterstitialManager() {

    private val counterMap by lazy { HashMap<String, Int>() }

    fun loadInterstitialAd(adType: InterAdKey, listener: InterstitialOnLoadCallBack? = null) {
        var interAdId = ""
        var isRemoteEnable = false

        when (adType) {
            InterAdKey.FEATURE_SAVE_NOTE -> {
                interAdId = getResString(R.string.admob_inter_before_main_id)
                isRemoteEnable = sharedPreferenceUtils.rcInterFeatureSaveNote != 0
            }
        }

        loadInterstitial(
            context = context,
            adType = adType.value,
            interId = interAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showInterstitialAd(
        activity: Activity?,
        adType: InterAdKey,
        listener: InterstitialOnShowCallBack? = null
    ) {
        showInterstitial(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener
        )
    }

    /**
     * @param adType   Key of the Ad, it should be unique id and should be case-sensitive
     * @param remoteCounter   Pass remote counter value, if the value is n, it will load on "n-1". In case of <= 2, it will load everytime
     * @param loadOnStart  Determine whether ad should be load on the very first time or not?
     *
     *  e.g. remoteCounter = 3, ad will  load on "n-1" = 2
     *      if (loadOnStart) {
     *          // 1, 0, 0, 1, 0, 0, 1, 0, 0 ... so on
     *      } else {
     *          // 0, 0, 1, 0, 0, 1, 0, 0, 1 ... so on
     *      }
     */

    fun loadInterstitialAd(
        adType: InterAdKey,
        remoteCounter: Int,
        loadOnStart: Boolean,
        listener: InterstitialOnLoadCallBack? = null
    ) {
        when (loadOnStart) {
            true -> counterMap.putIfAbsent(adType.value, remoteCounter - 1)
            false -> counterMap.putIfAbsent(adType.value, 0)
        }

        if (counterMap.containsKey(adType.value)) {
            val counter = counterMap[adType.value] ?: 0
            counterMap[adType.value] = counter + 1
            counterMap[adType.value]?.let { currentCounter ->
                Log.d(
                    TAG_ADS,
                    "$adType -> loadInterstitial_Counter ----- Total Counter: $remoteCounter, Current Counter: $currentCounter"
                )
                if (currentCounter >= remoteCounter - 1) {
                    counterMap[adType.value] = 0
                    loadInterstitialAd(adType = adType, listener = listener)
                }
            }
        }
    }

    private fun getResString(@StringRes resId: Int): String {
        return context?.resources?.getString(resId) ?: ""
    }
}