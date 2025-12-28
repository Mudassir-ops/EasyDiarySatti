package com.example.easydiarysatti.ads.cmp.callback

interface ConsentCallback {
    fun onAdsLoad(canRequestAd: Boolean) {}
    fun onConsentFormLoaded() {}
    fun onConsentFormDismissed() {}
    fun onPolicyStatus(required: Boolean) {}
}