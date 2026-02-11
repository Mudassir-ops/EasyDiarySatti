package com.example.easydiarysatti.ads.natives.domain.useCases

import android.content.Context
import android.util.Log
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.data.entities.ItemNativeAd
import com.example.easydiarysatti.ads.natives.data.repositories.RepositoryNativeImpl
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class UseCaseNative @Inject constructor(
    private val repositoryNativeImpl: RepositoryNativeImpl,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager,
    @ApplicationContext private val context: Context
) {

    @Volatile
    private var isAdLoading = false

    private fun checkRemoteConfig(nativeAdKey: NativeAdKey): Boolean {
        // Dynamic lookup: matches enum value (e.g., "nativeLogin") to JSON "name"
        return sharedPreferenceUtils.getAdShowStatus(nativeAdKey.value)
    }

    private fun getAdId(nativeAdKey: NativeAdKey): String {
        // Dynamic lookup: matches enum value to JSON "id"
        return sharedPreferenceUtils.getAdId(nativeAdKey.value)
    }

    fun loadNativeAd(
        nativeAdKey: NativeAdKey,
        callback: (ItemNativeAd?) -> Unit
    ) {
        loadNativeAd(
            nativeAdKey = nativeAdKey,
            callback = callback,
            loadAdAction = { adId ->
                isAdLoading = true
                repositoryNativeImpl.fetchNativeAd(
                    adKey = nativeAdKey.value,
                    adId = adId
                ) { itemNativeAd ->
                    isAdLoading = false
                    callback.invoke(itemNativeAd)
                }
            }
        )
    }

    private fun loadNativeAd(
        nativeAdKey: NativeAdKey,
        callback: (ItemNativeAd?) -> Unit,
        loadAdAction: (adId: String) -> Unit
    ) {
        val isRemoteEnable = checkRemoteConfig(nativeAdKey)
        val adId = getAdId(nativeAdKey)

        when {
            sharedPreferenceUtils.isAppPurchased -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Premium user")
                callback.invoke(null)
            }

            isRemoteEnable.not() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Remote config is off")
                callback.invoke(null)
            }

            internetManager.isInternetConnected.not() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: No Internet")
                callback.invoke(null)
            }

            adId.isEmpty() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Ad id is empty in JSON")
                callback.invoke(null)
            }

            isAdLoading -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Ad is already loading")
            }

            else -> {
                loadAdAction(adId)
            }
        }
    }

    fun destroyNative(nativeAdKey: NativeAdKey): Boolean {
        return repositoryNativeImpl.destroyNative(nativeAdKey.value)
    }
}