package com.example.easydiarysatti.ads.banner.domain.useCases

import android.content.Context
import android.util.Log
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.data.entities.ItemBannerAd
import com.example.easydiarysatti.ads.banner.data.repositories.RepositoryBannerImpl
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdType
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import com.google.android.gms.ads.AdView
import javax.inject.Inject


class UseCaseBanner @Inject constructor(
    private val repositoryBannerImpl: RepositoryBannerImpl,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager,
) {

    @Volatile
    private var isAdLoading = false

    // Fetches "show" status directly from JSON
    private fun checkRemoteConfig(bannerAdKey: BannerAdKey): Boolean {
        return sharedPreferenceUtils.getAdShowStatus(bannerAdKey.value)
    }

    // Fetches "id" directly from JSON
    private fun getAdId(bannerAdKey: BannerAdKey): String {
        return sharedPreferenceUtils.getAdId(bannerAdKey.value)
    }

    suspend fun loadBannerAd(
        adView: AdView,
        context: Context,
        bannerAdKey: BannerAdKey,
        bannerAdType: BannerAdType = BannerAdType.ADAPTIVE,
        callback: (ItemBannerAd?) -> Unit
    ) {
        val isRemoteEnable = checkRemoteConfig(bannerAdKey)
        val adId = getAdId(bannerAdKey)

        when {
            sharedPreferenceUtils.isAppPurchased -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Premium user")
                callback.invoke(null)
            }

            isRemoteEnable.not() || adId.isEmpty() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Disabled or ID missing in JSON")
                callback.invoke(null)
            }

            internetManager.isInternetConnected.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: No Internet")
                callback.invoke(null)
            }

            else -> {
                isAdLoading = true
                repositoryBannerImpl.fetchBannerAd(
                    adKey = bannerAdKey.value,
                    adId = adId,
                    bannerAdType = bannerAdType,
                    adView = adView
                ) {
                    isAdLoading = false
                    callback.invoke(it)
                }
            }
        }
    }

    fun destroyBanner(bannerAdKey: BannerAdKey): Boolean {
        return repositoryBannerImpl.destroyBanner(bannerAdKey.value)
    }
}