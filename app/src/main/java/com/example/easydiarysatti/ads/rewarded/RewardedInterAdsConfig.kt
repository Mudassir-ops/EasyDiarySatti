package com.example.easydiarysatti.ads.rewarded

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.example.easydiarysatti.ads.manager.InternetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.rewarded.enums.RewardedInterAdKey
import com.example.easydiarysatti.ads.rewarded.managers.RewardedInterManager
import javax.inject.Inject

class RewardedInterAdsConfig @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedInterManager() {

    fun loadRewardedInterAd(adType: RewardedInterAdKey, listener: RewardedOnLoadCallBack? = null) {
        // Fetch ID and Status dynamically from JSON
        val rewardedInterAdId = sharedPreferenceUtils.getAdId(adType.value)
        val isRemoteEnable = sharedPreferenceUtils.getAdShowStatus(adType.value)

        loadRewardedInter(
            context = context,
            adType = adType.value,
            rewardedInterId = rewardedInterAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showRewardedInterAd(
        activity: Activity?,
        adType: RewardedInterAdKey,
        listener: RewardedOnShowCallBack? = null
    ) {
        showRewardedInter(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener = listener
        )
    }
}