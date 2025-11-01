package com.example.easydiarysatti.di

import android.content.Context
import android.net.ConnectivityManager
import com.example.easydiarysatti.ads.appOpen.application.AppOpenAdManager
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.banner.data.dataSources.local.DataSourceLocalBanner
import com.example.easydiarysatti.ads.banner.data.dataSources.remote.DataSourceRemoteBanner
import com.example.easydiarysatti.ads.banner.data.repositories.RepositoryBannerImpl
import com.example.easydiarysatti.ads.interstitial.InterstitialAdsConfig
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.data.dataSources.local.DataSourceLocalNative
import com.example.easydiarysatti.ads.natives.data.dataSources.remote.DataSourceRemoteNative
import com.example.easydiarysatti.ads.natives.data.repositories.RepositoryNativeImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {

    @Provides
    @Singleton
    fun provideSharedPreferenceUtils(
        @ApplicationContext context: Context
    ): SharedPreferenceUtils {
        return SharedPreferenceUtils(
            context.getSharedPreferences("app_prefs_ads", Context.MODE_PRIVATE)
        )
    }

    @Provides
    @Singleton
    fun provideInternetManager(
        @ApplicationContext context: Context
    ): InternetManager {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return InternetManager(connectivityManager)
    }


    /* ---------------- App Open Ads ---------------- */
    @Provides
    @Singleton
    fun provideAppOpenAdManager(
        @ApplicationContext context: Context,
        sharedPreferenceUtils: SharedPreferenceUtils,
        internetManager: InternetManager
    ): AppOpenAdManager {
        return AppOpenAdManager(
            context = context,
            internetManager = internetManager,
            sharedPrefs = sharedPreferenceUtils
        )
    }

    @Provides
    @Singleton
    fun provideAppOpenAdsConfig(
        @ApplicationContext context: Context,
        internetManager: InternetManager,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): AppOpenAdsConfig {
        return AppOpenAdsConfig(
            context,
            sharedPreferenceUtils = sharedPreferenceUtils,
            internetManager = internetManager
        )
    }

    /* ---------------- Interstitial Ads ---------------- */
    @Provides
    @Singleton
    fun provideInterstitialAdsConfig(
        @ApplicationContext context: Context,
        internetManager: InternetManager,
        sharedPreferenceUtils: SharedPreferenceUtils
    ): InterstitialAdsConfig {
        return InterstitialAdsConfig(
            context,
            sharedPreferenceUtils = sharedPreferenceUtils,
            internetManager = internetManager
        )
    }

    /* ---------------- Banner Ads ---------------- */
    @Provides
    @Singleton
    fun provideDataSourceLocalBanner(): DataSourceLocalBanner = DataSourceLocalBanner()

    @Provides
    @Singleton
    fun provideDataSourceRemoteBanner(context: Context): DataSourceRemoteBanner =
        DataSourceRemoteBanner(context)

    @Provides
    @Singleton
    fun provideRepositoryBannerImpl(
        local: DataSourceLocalBanner,
        remote: DataSourceRemoteBanner
    ): RepositoryBannerImpl = RepositoryBannerImpl(local, remote)

    /* ---------------- Native Ads ---------------- */
    @Provides
    @Singleton
    fun provideDataSourceLocalNative(): DataSourceLocalNative = DataSourceLocalNative()

    @Provides
    @Singleton
    fun provideDataSourceRemoteNative(context: Context): DataSourceRemoteNative =
        DataSourceRemoteNative(context)

    @Provides
    @Singleton
    fun provideRepositoryNativeImpl(
        local: DataSourceLocalNative,
        remote: DataSourceRemoteNative
    ): RepositoryNativeImpl = RepositoryNativeImpl(local, remote)
}
