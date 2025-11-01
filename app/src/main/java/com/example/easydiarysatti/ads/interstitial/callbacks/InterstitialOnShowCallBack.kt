package com.example.easydiarysatti.ads.interstitial.callbacks

interface InterstitialOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onAdClicked() {}
    fun onAdImpressionDelayed() {}
}