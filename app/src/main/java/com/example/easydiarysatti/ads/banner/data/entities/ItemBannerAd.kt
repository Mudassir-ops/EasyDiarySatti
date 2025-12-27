package com.example.easydiarysatti.ads.banner.data.entities

import com.google.android.gms.ads.AdView

data class ItemBannerAd(
    val adId: String,
    val adView: AdView,
    val impressionReceived: Boolean = false
)