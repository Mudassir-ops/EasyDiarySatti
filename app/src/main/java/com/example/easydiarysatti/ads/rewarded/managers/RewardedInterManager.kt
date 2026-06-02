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
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.example.easydiarysatti.ads.AdShowingTracker
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

abstract class RewardedInterManager {

    // Per-key storage — same fix as RewardedManager.
    // Single mRewardedInterstitialAd slot caused cross-key contamination:
    // SAVE's failover ad occupied the slot → MEDIA/BG isRewardedInterLoaded() returned true
    // → onResponse(true) fired immediately → showed SAVE's ad or null → free access granted.
    private val rewardedInterAdMap   = HashMap<String, RewardedInterstitialAd>()
    private val loadingInterKeys     = HashSet<String>()
    private val pendingInterListeners = HashMap<String, RewardedOnLoadCallBack>()

    protected fun loadRewardedInter(
        context: Context?,
        adType: String,
        rewardedInterId: String,
        adEnable: Boolean,
        isAppPurchased: Boolean,
        isInternetConnected: Boolean,
        listener: RewardedOnLoadCallBack?,
    ) {

        if (isRewardedInterLoaded(adType)) {
            Log.i(TAG_ADS, "$adType -> loadRewardedInter: Already loaded")
            listener?.onResponse(true)
            return
        }

        if (loadingInterKeys.contains(adType)) {
            Log.d(TAG_ADS, "$adType -> loadRewardedInter: Ad is already loading, queuing listener...")
            pendingInterListeners[adType] = listener ?: return
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Premium user")
            listener?.onResponse(false)
            return
        }

        if (adEnable.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Remote config is off")
            listener?.onResponse(false)
            return
        }

        if (isInternetConnected.not()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Internet is not connected")
            listener?.onResponse(false)
            return
        }

        if (context == null) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Context is null")
            listener?.onResponse(false)
            return
        }

        if (rewardedInterId.trim().isEmpty()) {
            Log.e(TAG_ADS, "$adType -> loadRewardedInter: Ad id is empty")
            listener?.onResponse(false)
            return
        }

        Log.d(TAG_ADS, "$adType -> loadRewardedInter: Requesting admob server for ad...")
        loadingInterKeys.add(adType)

        RewardedInterstitialAd.load(
            context,
            rewardedInterId.trim(),
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG_ADS, "$adType -> loadRewardedInter: onAdFailedToLoad: ${adError.message}")
                    loadingInterKeys.remove(adType)
                    rewardedInterAdMap.remove(adType)
                    listener?.onResponse(false)
                    pendingInterListeners.remove(adType)?.onResponse(false)
                }

                override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                    Log.i(TAG_ADS, "$adType -> loadRewardedInter: onAdLoaded")
                    loadingInterKeys.remove(adType)
                    rewardedInterAdMap[adType] = rewardedInterstitialAd   // per-key slot
                    listener?.onResponse(true)
                    pendingInterListeners.remove(adType)?.onResponse(true)
                }
            })
    }

    protected fun showRewardedInter(
        activity: Activity?,
        adType: String,
        isAppPurchased: Boolean,
        listener: RewardedOnShowCallBack?
    ) {

        val mRewardedInterstitialAd = rewardedInterAdMap[adType]   // per-key lookup

        if (mRewardedInterstitialAd == null) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: RewardedInter is not loaded yet")
            listener?.onAdFailedToShow()
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: Premium user")
            rewardedInterAdMap.remove(adType)
            listener?.onAdFailedToShow()
            return
        }

        if (activity == null) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: activity reference is null")
            listener?.onAdFailedToShow()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG_ADS, "$adType -> showRewardedInter: activity is finishing or destroyed")
            listener?.onAdFailedToShow()
            return
        }

        mRewardedInterstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                AdShowingTracker.isAdShowing = true
                Log.d(TAG_ADS, "admob RewardedInter onAdShowedFullScreenContent")
                listener?.onAdShowedFullScreenContent()
                rewardedInterAdMap.remove(adType)
            }

            override fun onAdDismissedFullScreenContent() {
                AdShowingTracker.clearWithDelay()
                Log.d(TAG_ADS, "admob RewardedInter onAdDismissedFullScreenContent")
                listener?.onAdDismissedFullScreenContent()
                rewardedInterAdMap.remove(adType)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                AdShowingTracker.clearWithDelay()
                Log.e(TAG_ADS, "admob RewardedInter onAdFailedToShowFullScreenContent: ${adError.message}")
                listener?.onAdFailedToShow()
                rewardedInterAdMap.remove(adType)
            }

            override fun onAdImpression() {
                Log.v(TAG_ADS, "admob RewardedInter onAdImpression")
                listener?.onAdImpression()
            }
        }

        Log.d(TAG_ADS, "$adType -> RewardedInter: showing ad")
        // Set BEFORE show() so ProcessLifecycleOwner.onStop sees the flag
        // immediately when the rewarded interstitial Activity launches.
        AdShowingTracker.isAdShowing = true
        mRewardedInterstitialAd.show(activity) {
            Log.d(TAG_ADS, "admob RewardedInter onUserEarnedReward")
            listener?.onUserEarnedReward()
        }
    }

    fun isRewardedInterLoaded(adType: String): Boolean = rewardedInterAdMap.containsKey(adType)

    // Legacy no-arg check — true if any key has a loaded ad
    fun isRewardedInterLoaded(): Boolean = rewardedInterAdMap.isNotEmpty()
}