package com.example.easydiarysatti.ads.firebase

import android.util.Log
import com.example.easydiarysatti.BuildConfig
import com.example.easydiarysatti.ads.firebase.FirebaseUtils.recordException
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.Constants.TAG_REMOTE
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.get
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject

class RemoteConfiguration @Inject constructor(
    private val internetManager: InternetManager,
    private val sharedPreferenceUtils: SharedPreferenceUtils
) {

    private val remoteConfig: FirebaseRemoteConfig by lazy { Firebase.remoteConfig }

    init {
        val configSettings = remoteConfigSettings {
            setFetchTimeoutInSeconds(10)
            // 0L for Debug instant refresh, 3600L for Release (1 hour cache)
            setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun checkRemoteConfig(fetchCallback: (Boolean) -> Unit) {
        if (!internetManager.isInternetConnected) {
            fetchCallback.invoke(false)
            return
        }
        addLiveUpdateListener()
        fetchRemoteValues(fetchCallback)
    }

    private fun addLiveUpdateListener() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnSuccessListener { isSuccessful ->
                    if (isSuccessful) updateRemoteValues()
                }
            }
            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG_REMOTE, "RemoteConfig Update Error", error)
            }
        })
    }

    private fun fetchRemoteValues(callback: ((Boolean) -> Unit)?) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) updateRemoteValues()
            callback?.invoke(task.isSuccessful)
        }
    }

    private fun updateRemoteValues() {
        try {
            // ── Onboarding flow screen toggles (plan: Remote Config booleans) ──
            // Key: onboarding_slides_display  → controls isOnboardingEnabled
            sharedPreferenceUtils.isOnboardingEnabled =
                remoteConfig.getBoolean("onboarding_slides_display")

            // Key: permission_screen_display  → controls isPermissionEnabled
            sharedPreferenceUtils.isPermissionEnabled =
                remoteConfig.getBoolean("permission_screen_display")

            // Key: start_writing_display  → controls isNameWritingEnabled
            sharedPreferenceUtils.isNameWritingEnabled =
                remoteConfig.getBoolean("start_writing_display")

            // Key: pin_setup_screen_display  → controls isPinSetupEnabled
            sharedPreferenceUtils.isPinSetupEnabled =
                remoteConfig.getBoolean("pin_setup_screen_display")

            // Key: choose_your_theme_screen_display  → controls isThemeSelectionEnabled
            sharedPreferenceUtils.isThemeSelectionEnabled =
                remoteConfig.getBoolean("choose_your_theme_screen_display")

            // ── Splash timeout ────────────────────────────────────────────────
            // Key: splash_timeout  (Long, ms)
            val remoteTimeout = remoteConfig.getLong("splash_timeout")
            if (remoteTimeout > 0) {
                sharedPreferenceUtils.splashTimeout = remoteTimeout
            }

            // ── Library native ad frequency ───────────────────────────────────
            // Key: library_native_ad_after_items  (String number, e.g. "2")
            val libraryAdFrequency = remoteConfig.getString("library_native_ad_after_items")
            sharedPreferenceUtils.libraryNativeAdAfterItems =
                if (libraryAdFrequency.isNotEmpty()) libraryAdFrequency else "2"

            // ── Internet Connectivity Popup toggle ────────────────────────────
            // Key:     "internet_connectivity_display"
            // Type:    Boolean
            // Default: true
            // Effect:  true  → show no-internet popup at FTU (Add Note) and RU (Home)
            //          false → popup is suppressed app-wide
            sharedPreferenceUtils.internetConnectivityDisplay =
                         remoteConfig.getLong("internet_connectivity_display")
            Log.d(
                TAG_REMOTE,
                "RemoteConfig: Flags Updated → " +
                        "Onboard=${sharedPreferenceUtils.isOnboardingEnabled}, " +
                        "Permission=${sharedPreferenceUtils.isPermissionEnabled}, " +
                        "Name=${sharedPreferenceUtils.isNameWritingEnabled}, " +
                        "Pin=${sharedPreferenceUtils.isPinSetupEnabled}, " +
                        "Theme=${sharedPreferenceUtils.isThemeSelectionEnabled}, " +
                        "Timeout=${sharedPreferenceUtils.splashTimeout}, " +
                        "InternetPopup=${sharedPreferenceUtils.internetConnectivityDisplay}"

            )

            // ── IAA JSON (interstitials, app-opens, banners, natives) ─────────
            // Debug key:   dairy_ads_debug
            // Release key: dairy_ads_release
            val iaaKey  = if (BuildConfig.DEBUG) "dairy_ads_debug" else "dairy_ads_release"
            val iaaJson = remoteConfig.getString(iaaKey)
            if (iaaJson.isNotEmpty()) {
                sharedPreferenceUtils.iaaJson = iaaJson
                Log.d(TAG_REMOTE, "RemoteConfig: IAA JSON updated (key=$iaaKey)")
            }

            // ── IAP JSON (paywalls, remove-ads, rewarded gates) ───────────────
            // Debug key:   dairy_ads_iap_debug
            // Release key: dairy_ads_iap_release
            //
            // This JSON must contain Items with these names (from plan):
            //   onboarding_paywall_config  → Onboarding Paywall
            //   splash_paywall_config      → Splash Paywall
            //   main_paywall_config        → Main Paywall
            //   remove_ads_inter_ad_cross_ipu → Remove Ads Only popup
            //   free_add_note_save_quota   → Rewarded gate: save note
            //   free_add_note_media_quota  → Rewarded gate: media note
            val iapKey  = if (BuildConfig.DEBUG) "dairy_ads_iap_debug" else "dairy_ads_iap_release"
            val iapJson = remoteConfig.getString(iapKey)
            if (iapJson.isNotEmpty()) {
                sharedPreferenceUtils.iapJson = iapJson
                Log.d(TAG_REMOTE, "RemoteConfig: IAP JSON updated (key=$iapKey)")
            }

        } catch (ex: Exception) {
            Log.e(TAG_REMOTE, "RemoteConfiguration: updateRemoteValues error: ${ex.message}")
        }
    }
}