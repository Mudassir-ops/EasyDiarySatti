package com.example.easydiarysatti.ads.interstitial.manager

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnLoadCallBack
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.example.easydiarysatti.ads.AdShowingTracker
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

abstract class InterstitialManager {

    private var mInterstitialAd: InterstitialAd? = null
    private var isInterLoading = false

    protected fun loadInterstitial(
        context: Context?,
        adType: String,
        interId: String,
        adEnable: Boolean,
        isAppPurchased: Boolean,
        isInternetConnected: Boolean,
        listener: InterstitialOnLoadCallBack?,
    ) {

        if (isInterstitialLoaded()) {
            Log.i(TAG_ADS, "$adType -> loadInterstitial: Already loaded")
            listener?.onResponse(true)
            return
        }

        if (isInterLoading) {
            Log.d(TAG_ADS, "$adType -> loadInterstitial: Ad is already loading...")
            // No need to invoke callback, in some cases (e.g. activity recreation) it interrupts our response, as we are waiting for response in Splash
            // listener?.onResponse(false)  // Uncomment if u still need to listen this case
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> loadInterstitial: Premium user")
            listener?.onResponse(false)
            return
        }

        if (adEnable.not()) {
            Log.e(TAG_ADS, "$adType -> loadInterstitial: Remote config is off")
            listener?.onResponse(false)
            return
        }

        if (isInternetConnected.not()) {
            Log.e(TAG_ADS, "$adType -> loadInterstitial: Internet is not connected")
            listener?.onResponse(false)
            return
        }

        if (context == null) {
            Log.e(TAG_ADS, "$adType -> loadInterstitial: Context is null")
            listener?.onResponse(false)
            return
        }

        if (interId.trim().isEmpty()) {
            Log.e(TAG_ADS, "$adType -> loadInterstitial: Ad id is empty")
            listener?.onResponse(false)
            return
        }

        Log.d(TAG_ADS, "$adType -> loadInterstitial: Requesting admob server for ad...")
        isInterLoading = true

        InterstitialAd.load(
            context,
            interId.trim(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(
                        TAG_ADS,
                        "$adType -> loadInterstitial: onAdFailedToLoad: ${adError.message}"
                    )
                    isInterLoading = false
                    mInterstitialAd = null
                    listener?.onResponse(false)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.i(TAG_ADS, "$adType -> loadInterstitial: onAdLoaded")
                    isInterLoading = false
                    mInterstitialAd = interstitialAd
                    listener?.onResponse(true)
                }
            })
    }

    protected fun showInterstitial(
        activity: Activity?,
        adType: String,
        isAppPurchased: Boolean,
        listener: InterstitialOnShowCallBack?
    ) {

        if (isInterstitialLoaded().not()) {
            Log.e(TAG_ADS, "$adType -> showInterstitial: Interstitial is not loaded yet")
            listener?.onAdFailedToShow()
            return
        }

        if (isAppPurchased) {
            Log.e(TAG_ADS, "$adType -> showInterstitial: Premium user")
            if (isInterstitialLoaded()) {
                Log.d(TAG_ADS, "$adType -> Destroying loaded inter ad due to Premium user")
                mInterstitialAd = null
            }
            listener?.onAdFailedToShow()
            return
        }

        if (activity == null) {
            Log.e(TAG_ADS, "$adType -> showInterstitial: activity reference is null")
            listener?.onAdFailedToShow()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG_ADS, "$adType -> showInterstitial: activity is finishing or destroyed")
            listener?.onAdFailedToShow()
            return
        }

        Log.d(TAG_ADS, "$adType -> showInterstitial: showing ad")

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdShowedFullScreenContent() {
                // Ad is physically on screen — block app-open ad immediately.
                AdShowingTracker.isAdShowing = true
                Log.d(TAG_ADS, "$adType -> showInterstitial: onAdShowedFullScreenContent")
                listener?.onAdShowedFullScreenContent()
            }

            override fun onAdDismissedFullScreenContent() {
                // Null HERE (not in onAdImpression) so isInterstitialLoaded() stays
                // true while the ad is still visible. On Android 11, nulling in
                // onAdImpression caused the next showInterstitialWithDialog call to
                // think no ad was loaded and fire onAdFailedToShow immediately,
                // leaving the user stuck on CreateNotesFragment.
                mInterstitialAd = null
                AdShowingTracker.clearWithDelay()
                Log.d(TAG_ADS, "$adType -> showInterstitial: onAdDismissedFullScreenContent")
                listener?.onAdDismissedFullScreenContent()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                mInterstitialAd = null
                AdShowingTracker.clearWithDelay()
                Log.e(TAG_ADS, "$adType -> showInterstitial: onAdFailedToShowFullScreenContent: ${adError.code}")
                listener?.onAdFailedToShow()
            }

            override fun onAdClicked() {
                listener?.onAdClicked()
            }

            override fun onAdImpression() {
                // Do NOT null mInterstitialAd here — ad is still showing.
                // Do NOT postDelayed onAdImpressionDelayed — on Android 11 this
                // 300ms callback fires into the NEXT ad's lifecycle, calling
                // checkRemoveAdsPopupOrNavigate() at the wrong time.
                Log.v(TAG_ADS, "$adType -> showInterstitial: onAdImpression")
                listener?.onAdImpression()
            }
        }
        // Set BEFORE show() so ProcessLifecycleOwner.onStop sees the flag
        // immediately when the interstitial Activity launches.
        AdShowingTracker.isAdShowing = true
        mInterstitialAd?.show(activity)
    }

    fun isInterstitialLoaded(): Boolean {
        return mInterstitialAd != null
    }
}