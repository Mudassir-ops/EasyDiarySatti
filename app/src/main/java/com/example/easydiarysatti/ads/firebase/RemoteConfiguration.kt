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
            // ── Onboarding Flow JSON ──────────────────────────────────────────
            // Remote Config key: "onboarding_flow"
            // Value: the full onboarding_flow JSON (first_open + returning_open cases).
            // The JSON replaces all individual boolean screen-toggle keys.
            // If the remote value is empty we keep whatever is already stored
            // (defaults to DEFAULT_ONBOARDING_FLOW_JSON on a fresh install).
            val remoteOnboardingFlow = remoteConfig.getString("onboarding_flow")
            if (remoteOnboardingFlow.isNotEmpty()) {
                sharedPreferenceUtils.onboardingFlowJson = remoteOnboardingFlow
                Log.d(TAG_REMOTE, "RemoteConfig: onboarding_flow JSON updated")
            }

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
            // Key:  "internet_connectivity_display"
            // Type: Long
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
            val iaaKey  = if (BuildConfig.DEBUG) "dairy_ads_debug" else "dairy_ads_release"
            val iaaJson = remoteConfig.getString(iaaKey)
            if (iaaJson.isNotEmpty()) {
                sharedPreferenceUtils.iaaJson = iaaJson
                Log.d(TAG_REMOTE, "RemoteConfig: IAA JSON updated (key=$iaaKey)")
            }

            // ── IAP JSON (paywalls, remove-ads, rewarded gates) ───────────────
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