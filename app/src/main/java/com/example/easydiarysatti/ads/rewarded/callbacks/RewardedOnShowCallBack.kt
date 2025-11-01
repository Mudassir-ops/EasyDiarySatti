package com.example.easydiarysatti.ads.rewarded.callbacks

interface RewardedOnShowCallBack {
    fun onAdDismissedFullScreenContent() {}
    fun onAdFailedToShow()
    fun onAdShowedFullScreenContent() {}
    fun onAdImpression() {}
    fun onUserEarnedReward()
}