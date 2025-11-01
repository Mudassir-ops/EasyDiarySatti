package com.example.easydiarysatti.ads.natives.data.dataSources.remote

import android.content.Context
import android.util.Log
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.example.easydiarysatti.ads.natives.data.entities.ItemNativeAd

class DataSourceRemoteNative(private val context: Context) {

    fun fetchNativeAd(adKey: String, adId: String, callback: (ItemNativeAd?) -> Unit) {
        val nativeBuilderOption = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .build()

        var nativeAd: NativeAd? = null

        val adLoader = AdLoader.Builder(context, adId)
            .forNativeAd { unifiedNativeAd ->
                nativeAd = unifiedNativeAd
                callback.invoke(ItemNativeAd(adId, unifiedNativeAd))
            }
            .withAdListener(object : AdListener() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.v(TAG_ADS, "$adKey -> loadNative: onAdImpression")
                    nativeAd?.let {
                        callback.invoke(ItemNativeAd(adId, it, true))
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.e(TAG_ADS, "$adKey -> loadNative: onAdFailedToLoad: ${loadAdError.message}")
                    callback.invoke(null)
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.i(TAG_ADS, "$adKey -> loadNative: onAdLoaded")
                }
            })
            .withNativeAdOptions(nativeBuilderOption)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
        Log.d(TAG_ADS, "$adKey -> loadNative: Requesting admob server for ad...")
    }
}