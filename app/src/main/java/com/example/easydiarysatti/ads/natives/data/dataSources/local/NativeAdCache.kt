package com.example.easydiarysatti.ads.natives.data.dataSources.local

import com.example.easydiarysatti.ads.natives.data.entities.ItemNativeAd
import java.util.concurrent.ConcurrentHashMap

class NativeAdCache {

    private val adCache = ConcurrentHashMap<String, ItemNativeAd>()

    /**
     * Return cached ad if it exists
     */
    fun getAd(adKey: String): ItemNativeAd? {
        return adCache[adKey]
    }

    /**
     * Return cached ad if it exists
     */
    fun getImpressionFreeAd(adKey: String): ItemNativeAd? {
        return adCache[adKey]?.takeIf { !it.impressionReceived }
    }

    /**
     * Store ad for cache
     */
    fun putAd(adKey: String, itemNativeAd: ItemNativeAd) {
        adCache[adKey] = itemNativeAd
    }

    /**
     * Find any free ad (an ad that hasn't received an impression).
     */
    fun getFreeAd(): ItemNativeAd? {
        return adCache.values.firstOrNull { !it.impressionReceived }
    }

    /**
     *  Only delete if impression is received else if ignore
     */
    fun deleteAd(adKey: String): Boolean {
        adCache[adKey]?.let {
            if (it.impressionReceived) {
                it.nativeAd.destroy()
                adCache.remove(adKey)
                return true
            }
        }
        return false
    }
}