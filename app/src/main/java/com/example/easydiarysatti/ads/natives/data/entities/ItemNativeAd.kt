package com.example.easydiarysatti.ads.natives.data.entities

import com.google.android.gms.ads.nativead.NativeAd

data class ItemNativeAd(
    val adId: String,
    val nativeAd: NativeAd,
    val impressionReceived: Boolean = false
)