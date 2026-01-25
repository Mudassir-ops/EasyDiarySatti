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

    val appOpen = "app_open"
    val bannerHome = "banner_home"
    val bannerOnboarding = "banner_onboarding"
    val interSaveNoteFeature = "inter_save_note_feature"
    val rewardedImagesOnNoteFeature = "rewarded_images_on_note_feature"

    val rewardedInterAiFeature = "rewardedInterAiFeature"

    // Native Ad Keys
    val nativeLanguage = "nativeLanguage"
    val nativeLogin = "native_login"
    val nativeFeature = "nativeFeature"
    val nativeHome = "nativeHome"
    val nativeExit = "nativeExit"
    val nativePermission = "nativePermission"
    val nativeName = "nativeName"
    val nativeSignup = "nativeSignup"
    val nativeLibrary = "nativeLibrary"
    val nativeCalendar = "nativeCalendar"
    val nativeProfile = "nativeProfile"
    val nativeTags = "nativeTags"
    val nativeEditTag = "nativeEditTag"
    val nativeReminder = "nativeReminder"
    val nativeChangePassword = "nativeChangePassword"

    /* ----- AppOpen Ads ----- */

    var rcAppOpen: Int
        get() = sharedPreferences.getInt(appOpen, 0)
        set(value) {
            sharedPreferences.edit().putInt(appOpen, value).apply()
        }

    /* ----- Banner Ads ----- */

    var rcBannerHome: Int
        get() = sharedPreferences.getInt(bannerHome, 0)
        set(value) {
            sharedPreferences.edit().putInt(bannerHome, value).apply()
        }

    var rcBannerOnboarding: Int
        get() = sharedPreferences.getInt(bannerOnboarding, 1)
        set(value) {
            sharedPreferences.edit().putInt(bannerOnboarding, value).apply()
        }

    /* ----- Interstitial Ads ----- */

    var rcInterFeatureSaveNote: Int
        get() = sharedPreferences.getInt(interSaveNoteFeature, 1)
        set(value) {
            sharedPreferences.edit().putInt(interSaveNoteFeature, value).apply()
        }

    /* ----- Rewarded Ads ----- */

    var rcRewardedImageAddFeature: Int
        get() = sharedPreferences.getInt(rewardedImagesOnNoteFeature, 1)
        set(value) {
            sharedPreferences.edit().putInt(rewardedImagesOnNoteFeature, value).apply()
        }

    var rcRewardedInterAiFeature: Int
        get() = sharedPreferences.getInt(rewardedInterAiFeature, 0)
        set(value) {
            sharedPreferences.edit().putInt(rewardedInterAiFeature, value).apply()
        }

    /* ----- Native Ads (Updated with new placements) ----- */

    var rcNativeLanguage: Int
        get() = sharedPreferences.getInt(nativeLanguage, 1)
        set(value) = sharedPreferences.edit().putInt(nativeLanguage, value).apply()

    var rcNativeLogin: Int
        get() = sharedPreferences.getInt(nativeLogin, 1)
        set(value) = sharedPreferences.edit().putInt(nativeLogin, value).apply()

    var rcNativeHome: Int
        get() = sharedPreferences.getInt(nativeHome, 1)
        set(value) = sharedPreferences.edit().putInt(nativeHome, value).apply()

    var rcNativeFeature: Int
        get() = sharedPreferences.getInt(nativeFeature, 0)
        set(value) = sharedPreferences.edit().putInt(nativeFeature, value).apply()

    var rcNativeExit: Int
        get() = sharedPreferences.getInt(nativeExit, 0)
        set(value) = sharedPreferences.edit().putInt(nativeExit, value).apply()

    var rcNativePermission: Int
        get() = sharedPreferences.getInt(nativePermission, 1)
        set(value) = sharedPreferences.edit().putInt(nativePermission, value).apply()

    var rcNativeName: Int
        get() = sharedPreferences.getInt(nativeName, 1)
        set(value) = sharedPreferences.edit().putInt(nativeName, value).apply()

    var rcNativeSignup: Int
        get() = sharedPreferences.getInt(nativeSignup, 1)
        set(value) = sharedPreferences.edit().putInt(nativeSignup, value).apply()

    var rcNativeLibrary: Int
        get() = sharedPreferences.getInt(nativeLibrary, 1)
        set(value) = sharedPreferences.edit().putInt(nativeLibrary, value).apply()

    var rcNativeCalendar: Int
        get() = sharedPreferences.getInt(nativeCalendar, 1)
        set(value) = sharedPreferences.edit().putInt(nativeCalendar, value).apply()

    var rcNativeProfile: Int
        get() = sharedPreferences.getInt(nativeProfile, 1)
        set(value) = sharedPreferences.edit().putInt(nativeProfile, value).apply()

    var rcNativeTags: Int
        get() = sharedPreferences.getInt(nativeTags, 1)
        set(value) = sharedPreferences.edit().putInt(nativeTags, value).apply()

    var rcNativeEditTag: Int
        get() = sharedPreferences.getInt(nativeEditTag, 1)
        set(value) = sharedPreferences.edit().putInt(nativeEditTag, value).apply()

    var rcNativeReminder: Int
        get() = sharedPreferences.getInt(nativeReminder, 1)
        set(value) = sharedPreferences.edit().putInt(nativeReminder, value).apply()

    var rcNativeChangePassword: Int
        get() = sharedPreferences.getInt(nativeChangePassword, 1)
        set(value) = sharedPreferences.edit().putInt(nativeChangePassword, value).apply()
}