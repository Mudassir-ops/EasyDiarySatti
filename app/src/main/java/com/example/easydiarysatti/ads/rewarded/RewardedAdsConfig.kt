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
import javax.inject.Inject


/**
 * @param context: Can be of application class
 */
class RewardedAdsConfig @Inject constructor(
    private val context: Context?,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager
) : RewardedManager() {

    fun loadRewardedAd(adType: RewardedAdKey, listener: RewardedOnLoadCallBack? = null) {
        var rewardedAdId = ""
        var isRemoteEnable = false

        when (adType) {
            RewardedAdKey.IMAGE_MORE_THAN_ONE -> {
                rewardedAdId = getResString(R.string.admob_rewarded_images_more_than_two)
                isRemoteEnable = sharedPreferenceUtils.rcRewardedImageAddFeature != 0
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