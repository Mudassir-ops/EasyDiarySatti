package com.example.easydiarysatti.ads.rewarded.managers

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.example.easydiarysatti.ads.AdShowingTracker
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

abstract class RewardedManager {

    // Per-key storage — each adType gets its own RewardedAd slot.
    // Previously a single mRewardedAd was shared across all keys (save/media/bg),
    // so loading save-RV then showing it would null the slot, leaving media/bg with nothing.
    private val rewardedAdMap = HashMap<String, RewardedAd>()
    private val loadingKeys   = HashSet<String>()
    private val pendingListeners = HashMap<String, RewardedOnLoadCallBack>()

    protected fun loadRewarded(
        context: Context?,
        adType: String,
        rewardedId: String,
        adEnable: Boolean,
        isAppPurchased: Boolean,
        isInternetConnected: Boolean,
        listener: RewardedOnLoadCallBack?,
    ) {

        Log.d(TAG_ADS, "═══════════════════════════════════")
        Log.d(TAG_ADS, "$adType -> loadRewarded: START")

        if (isRewardedLoaded(adType)) {
            Log.i(TAG_ADS, "$adType -> loadRewarded: Already loaded")
            listener?.onResponse(true)
            return
        }

        if (loadingKeys.contains(adType)) {
            Log.d(TAG_ADS, "$adType -> loadRewarded: Ad is already loading, queuing listener...")
            pendingListeners[adType] = listener ?: return
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> loadRewarded: Premium user")
            listener?.onResponse(false)
            return
        }

        if (adEnable.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewarded: Remote config is off")
            listener?.onResponse(false)
            return
        }

        if (isInternetConnected.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewarded: Internet is not connected")
            listener?.onResponse(false)
            return
        }

        if (context == null) {
            Log.e(TAG_ADS, "$adType -> loadRewarded: Context is null")
            listener?.onResponse(false)
            return
        }

        if (rewardedId.trim().isEmpty()) {
            Log.e(TAG_ADS, "$adType -> loadRewarded: Ad id is empty")
            listener?.onResponse(false)
            return
        }

        Log.d(TAG_ADS, "$adType -> loadRewarded: ✅ All checks passed!")
        Log.d(TAG_ADS, "$adType -> loadRewarded: Ad ID = $rewardedId")
        Log.d(TAG_ADS, "$adType -> loadRewarded: Requesting admob server for ad...")
        loadingKeys.add(adType)

        RewardedAd.load(
            context,
            rewardedId.trim(),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG_ADS, "═══════════════════════════════════")
                    Log.e(TAG_ADS, "$adType -> ❌ onAdFailedToLoad")
                    Log.e(TAG_ADS, "$adType -> Error Code: ${adError.code}")
                    Log.e(TAG_ADS, "$adType -> Error Message: ${adError.message}")
                    Log.e(TAG_ADS, "$adType -> Error Domain: ${adError.domain}")
                    Log.e(TAG_ADS, "$adType -> Error Cause: ${adError.cause}")
                    Log.e(TAG_ADS, "═══════════════════════════════════")
                    loadingKeys.remove(adType)
                    rewardedAdMap.remove(adType)
                    listener?.onResponse(false)
                    pendingListeners.remove(adType)?.onResponse(false)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.i(TAG_ADS, "═══════════════════════════════════")
                    Log.i(TAG_ADS, "$adType -> ✅ onAdLoaded SUCCESS!")
                    Log.i(TAG_ADS, "$adType -> Ad object: $rewardedAd")
                    Log.i(TAG_ADS, "═══════════════════════════════════")
                    loadingKeys.remove(adType)
                    rewardedAdMap[adType] = rewardedAd   // store per key
                    listener?.onResponse(true)
                    pendingListeners.remove(adType)?.onResponse(true)
                }
            })
    }

    protected fun showRewarded(
        activity: Activity?,
        adType: String,
        isAppPurchased: Boolean,
        listener: RewardedOnShowCallBack?
    ) {

        val mRewardedAd = rewardedAdMap[adType]   // per-key lookup

        if (mRewardedAd == null) {
            Log.e(TAG_ADS, "$adType -> showRewarded: Rewarded is not loaded yet")
            listener?.onAdFailedToShow()
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> showRewarded: Premium user")
            rewardedAdMap.remove(adType)
            listener?.onAdFailedToShow()
            return
        }

        if (activity == null) {
            Log.e(TAG_ADS, "$adType -> showRewarded: activity reference is null")
            listener?.onAdFailedToShow()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG_ADS, "$adType -> showRewarded: activity is finishing or destroyed")
            listener?.onAdFailedToShow()
            return
        }

        mRewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdShowingTracker.isAdShowing = true
                Log.d(TAG_ADS, "admob Rewarded onAdShowedFullScreenContent")
                listener?.onAdShowedFullScreenContent()
                rewardedAdMap.remove(adType)
            }

            override fun onAdDismissedFullScreenContent() {
                AdShowingTracker.clearWithDelay()
                Log.d(TAG_ADS, "admob Rewarded onAdDismissedFullScreenContent")
                listener?.onAdDismissedFullScreenContent()
                rewardedAdMap.remove(adType)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdShowingTracker.clearWithDelay()
                Log.e(TAG_ADS, "admob Rewarded onAdFailedToShowFullScreenContent: ${adError.message}")
                listener?.onAdFailedToShow()
                rewardedAdMap.remove(adType)
            }

            override fun onAdImpression() {
                Log.v(TAG_ADS, "admob Rewarded onAdImpression")
                listener?.onAdImpression()
            }
        }

        Log.d(TAG_ADS, "$adType -> Rewarded: showing ad")
        // Set BEFORE show() so ProcessLifecycleOwner.onStop sees the flag
        // immediately when the rewarded ad Activity launches — before any
        // FullScreenContentCallback fires.
        AdShowingTracker.isAdShowing = true
        mRewardedAd.show(activity) {
            Log.d(TAG_ADS, "admob Rewarded onUserEarnedReward")
            listener?.onUserEarnedReward()
        }
    }

    fun isRewardedLoaded(adType: String): Boolean {
        return rewardedAdMap.containsKey(adType)
    }

    // Legacy no-arg version — kept for any call sites that check without a key
    fun isRewardedLoaded(): Boolean = rewardedAdMap.isNotEmpty()
}