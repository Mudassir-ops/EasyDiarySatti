package com.example.easydiarysatti.ads.rewarded

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.rewarded.enums.RewardedInterAdKey
import com.example.easydiarysatti.ads.rewarded.managers.RewardedInterManager


/**
 * @param context: Can be of application class
 */
class RewardedInterAdsConfig(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedInterManager() {

    fun loadRewardedInterAd(adType: RewardedInterAdKey, listener: RewardedOnLoadCallBack? = null) {
        var rewardedInterAdId = ""
        var isRemoteEnable = false

        when (adType) {
            RewardedInterAdKey.AI_FEATURE -> {
                rewardedInterAdId = getResString(R.string.admob_rewarded_inter_ai_feature_id)
                isRemoteEnable = sharedPreferenceUtils.rcRewardedInterAiFeature != 0
            }
        }

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
            listener
        )
    }

    private fun getResString(@StringRes resId: Int): String {
        return context?.resources?.getString(resId) ?: ""
    }
}