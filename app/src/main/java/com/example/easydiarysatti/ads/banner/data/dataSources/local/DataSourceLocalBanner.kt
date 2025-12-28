package com.example.easydiarysatti.ads.banner.data.dataSources.local

import com.example.easydiarysatti.ads.banner.data.entities.ItemBannerAd


class DataSourceLocalBanner {

    private val bannerAdCache by lazy { BannerAdCache() }

    /**
     * Fetch a cached banner ad for the given key.
     * Returns the cached ad if available and not yet used (impression not received).
     * If no such ad exists, it tries to find a free ad (unused ad without impressions).
     */
    fun getCachedBannerAd(adKey: String): ItemBannerAd? {
        bannerAdCache.getImpressionFreeAd(adKey)?.let {
            return it
        }
        return bannerAdCache.getFreeAd() ?: bannerAdCache.getAd(adKey)
    }

    /**
     * Cache the given banner ad using the specified key.
     */
    fun putCachedBannerAd(adKey: String, itemBannerAd: ItemBannerAd) {
        bannerAdCache.putAd(adKey, itemBannerAd)
    }

    /**
     * Remove the cached banner ad for the given key if it has been used (impression received).
     * This ensures only used ads are removed from the cache.
     */
    fun destroyBanner(adKey: String): Boolean {
        return bannerAdCache.deleteAd(adKey)
    }
}