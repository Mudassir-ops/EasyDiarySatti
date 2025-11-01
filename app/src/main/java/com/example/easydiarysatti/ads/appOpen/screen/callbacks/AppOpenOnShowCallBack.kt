package com.example.easydiarysatti.ads.appOpen.screen.callbacks

interface AppOpenOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onAdImpressionDelayed() {}
    fun onAdClicked() {}
}