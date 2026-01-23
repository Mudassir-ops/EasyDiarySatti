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

    private fun checkRemoteConfig(bannerAdKey: BannerAdKey): Boolean {
        return when (bannerAdKey) {
            BannerAdKey.HOME -> sharedPreferenceUtils.rcBannerHome != 0
            BannerAdKey.ON_BOARDING -> sharedPreferenceUtils.rcBannerOnboarding != 0
        }
    }

    private fun getAdId(bannerAdKey: BannerAdKey, context: Context): String {
        return when (bannerAdKey) {
            BannerAdKey.HOME -> context.getString(R.string.admob_banner_home_id).trim()
            BannerAdKey.ON_BOARDING -> context.getString(R.string.admob_banner_onboard).trim()
        }
    }

    private fun getAdType(bannerAdKey: BannerAdKey): BannerAdType {
        return when (bannerAdKey) {
            BannerAdKey.HOME -> BannerAdType.COLLAPSIBLE_BOTTOM
            BannerAdKey.ON_BOARDING -> BannerAdType.COLLAPSIBLE_BOTTOM
        }
    }

    fun loadBannerAd(
        adView: AdView,
        context: Context,
        bannerAdKey: BannerAdKey,
        callback: (ItemBannerAd?) -> Unit
    ) {
        val bannerAdType = getAdType(bannerAdKey)
        validateAndLoadAd(bannerAdKey, context, callback) { adId ->
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

    private fun validateAndLoadAd(
        bannerAdKey: BannerAdKey,
        context: Context,
        callback: (ItemBannerAd?) -> Unit,
        loadAdAction: (adId: String) -> Unit
    ) {
        val isRemoteEnable = checkRemoteConfig(bannerAdKey)
        val adId = getAdId(bannerAdKey, context)

        when {
            sharedPreferenceUtils.isAppPurchased -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Premium user")
                callback.invoke(null)
            }

            isRemoteEnable.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Remote config is off")
                callback.invoke(null)
            }

            internetManager.isInternetConnected.not() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Internet is not connected")
                callback.invoke(null)
            }

            adId.isEmpty() -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Ad id is empty")
                callback.invoke(null)
            }

            isAdLoading -> {
                Log.e(TAG_ADS, "${bannerAdKey.value} -> loadBanner: Ad is already loading")
                //callback.invoke(null)
            }

            else -> {
                loadAdAction(adId)
            }
        }
    }

    fun destroyBanner(bannerAdKey: BannerAdKey): Boolean {
        val isDestroyed = repositoryBannerImpl.destroyBanner(bannerAdKey.value)
        if (isDestroyed)
            Log.e(TAG_ADS, "${bannerAdKey.value} -> destroyBanner: destroyed")
        return isDestroyed
    }
}