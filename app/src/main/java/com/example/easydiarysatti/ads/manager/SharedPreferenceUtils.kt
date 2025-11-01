package com.example.easydiarysatti.ads.manager

import android.content.SharedPreferences

class SharedPreferenceUtils(private val sharedPreferences: SharedPreferences) {

    private val billingRequireKey = "isAppPurchased"
    private val isShowFirstScreenKey = "showFirstScreen"

    /* ---------- Billing ---------- */

    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(billingRequireKey, false)
        set(value) {
            sharedPreferences.edit().apply {
                putBoolean(billingRequireKey, value)
                apply()
            }
        }

    /* ---------- UI ---------- */

    var showFirstScreen: Boolean
        get() = sharedPreferences.getBoolean(isShowFirstScreenKey, true)
        set(value) {
            sharedPreferences.edit().apply {
                putBoolean(isShowFirstScreenKey, value)
                apply()
            }
        }

    /* ---------------------------------------- Ads ---------------------------------------- */

    val appOpen = "appOpen"
    val bannerHome = "bannerHome"
    val interSaveNoteFeature = "interSaveNoteFeature"
    val rewardedImagesOnNoteFeature = "rewardedImagesOnNoteFeature"


    val rewardedInterAiFeature = "rewardedInterAiFeature"
    val nativeLanguage = "nativeLanguage"
    val nativeOnBoarding = "nativeOnBoarding"
    val nativeFeature = "nativeFeature"
    val nativeHome = "nativeHome"
    val nativeExit = "nativeExit"

    /* ----- AppOpen Ads ----- */

    var rcAppOpen: Int
        get() = sharedPreferences.getInt(appOpen, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(appOpen, value)
                apply()
            }
        }


    /* ----- Banner Ads ----- */

    var rcBannerHome: Int
        get() = sharedPreferences.getInt(bannerHome, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(bannerHome, value)
                apply()
            }
        }

    /* ----- Interstitial Ads ----- */

    var rcInterFeatureSaveNote: Int
        get() = sharedPreferences.getInt(interSaveNoteFeature, 1)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(interSaveNoteFeature, value)
                apply()
            }
        }

    /* ----- Rewarded Ads ----- */

    var rcRewardedImageAddFeature: Int
        get() = sharedPreferences.getInt(rewardedImagesOnNoteFeature, 1)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(rewardedImagesOnNoteFeature, value)
                apply()
            }
        }

    var rcRewardedInterAiFeature: Int
        get() = sharedPreferences.getInt(rewardedInterAiFeature, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(rewardedInterAiFeature, value)
                apply()
            }
        }

    /* ----- Native Ads ----- */

    var rcNativeLanguage: Int
        get() = sharedPreferences.getInt(nativeLanguage, 1)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(nativeLanguage, value)
                apply()
            }
        }

    var rcNativeOnBoarding: Int
        get() = sharedPreferences.getInt(nativeOnBoarding, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(nativeOnBoarding, value)
                apply()
            }
        }

    var rcNativeHome: Int
        get() = sharedPreferences.getInt(nativeHome, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(nativeHome, value)
                apply()
            }
        }

    var rcNativeFeature: Int
        get() = sharedPreferences.getInt(nativeFeature, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(nativeFeature, value)
                apply()
            }
        }

    var rcNativeExit: Int
        get() = sharedPreferences.getInt(nativeExit, 0)
        set(value) {
            sharedPreferences.edit().apply {
                putInt(nativeExit, value)
                apply()
            }
        }
}