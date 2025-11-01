package com.example.easydiarysatti.ads.rewarded

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.rewarded.enums.RewardedAdKey
import com.example.easydiarysatti.ads.rewarded.managers.RewardedManager


/**
 * @param context: Can be of application class
 */
class RewardedAdsConfig(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedManager() {

    fun loadRewardedAd(adType: RewardedAdKey, listener: RewardedOnLoadCallBack? = null) {
        var rewardedAdId = ""
        var isRemoteEnable = false

        when (adType) {
            RewardedAdKey.AI_FEATURE -> {
                rewardedAdId = getResString(R.string.admob_rewarded_ai_feature_id)
                isRemoteEnable = sharedPreferenceUtils.rcRewardedAiFeature != 0
            }
        }

        loadRewarded(
            context = context,
            adType = adType.value,
            rewardedId = rewardedAdId,
            adEnable = isRemoteEnable,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            isInternetConnected = internetManager.isInternetConnected,
            listener = listener
        )
    }

    fun showRewardedAd(
        activity: Activity?,
        adType: RewardedAdKey,
        listener: RewardedOnShowCallBack? = null
    ) {
        showRewarded(
            activity = activity,
            adType = adType.value,
            isAppPurchased = sharedPreferenceUtils.isAppPurchased,
            listener
        )
    }

    private fun getResString(@StringRes resId: Int): String {
        return context?.resources?.getString(resId) ?: ""
    }
}