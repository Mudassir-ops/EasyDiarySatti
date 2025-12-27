package com.example.easydiarysatti.ads.banner.domain.repositories

import com.example.easydiarysatti.ads.banner.data.entities.ItemBannerAd
import com.google.android.gms.ads.AdView
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdType

interface RepositoryBanner {
    fun fetchBannerAd(
        adView: AdView,
        adKey: String,
        adId: String,
        bannerAdType: BannerAdType,
        callback: (ItemBannerAd?) -> Unit
    )

    fun destroyBanner(adKey: String): Boolean
}