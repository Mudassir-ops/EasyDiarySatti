package com.example.easydiarysatti.ads.natives.data.repositories

import android.util.Log
import com.example.easydiarysatti.ads.natives.data.dataSources.local.DataSourceLocalNative
import com.example.easydiarysatti.ads.natives.data.entities.ItemNativeAd
import com.example.easydiarysatti.ads.utils.Constants
import com.example.easydiarysatti.ads.natives.data.dataSources.remote.DataSourceRemoteNative
import com.example.easydiarysatti.ads.natives.domain.repository.RepositoryNative

class RepositoryNativeImpl(
    private val dataSourceLocalNative: DataSourceLocalNative,
    private val dataSourceRemoteNative: DataSourceRemoteNative
) : RepositoryNative {

    override fun fetchNativeAd(adKey: String, adId: String, callback: (ItemNativeAd?) -> Unit) {

        // Check cache resource
        dataSourceLocalNative.getCachedNativeAd(adKey)?.let { cachedAd ->
            Log.d(Constants.TAG_ADS, "$adKey -> fetchNativeAd: Reshowing Ad")
            callback.invoke(cachedAd)
            return
        }

        dataSourceRemoteNative.fetchNativeAd(adKey = adKey, adId = adId) { remoteAd ->
            remoteAd?.let {
                dataSourceLocalNative.putCachedNativeAd(adKey, it)
            }
            callback.invoke(remoteAd)
        }
    }

    override fun destroyNative(adKey: String): Boolean {
        return dataSourceLocalNative.destroyNative(adKey)
    }
}