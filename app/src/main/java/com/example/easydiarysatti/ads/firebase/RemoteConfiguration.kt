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
            // 0L for Debug to get instant changes, 3600L for Release
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
            if (task.isSuccessful) {
                updateRemoteValues()
            }
            callback?.invoke(task.isSuccessful)
        }
    }
    private fun updateRemoteValues() {
        try {
            // 1. Fetch and save the Onboarding display flag
            val showOnboarding = remoteConfig.getBoolean("onboarding_slides_display")
            sharedPreferenceUtils.isOnboardingEnabled = showOnboarding
            Log.d(TAG_REMOTE, "RemoteConfig: Onboarding Enabled = $showOnboarding")
            val showPermission = remoteConfig.getBoolean("permission_screen_display")
            sharedPreferenceUtils.isPermissionEnabled = showPermission
            val remoteTimeout = remoteConfig.getLong("splash_timeout")
            if (remoteTimeout > 0) {
                sharedPreferenceUtils.splashTimeout = remoteTimeout
            }
            val showNameWriting = remoteConfig.getBoolean("start_writing_display")
            sharedPreferenceUtils.isNameWritingEnabled = showNameWriting

            val showPinSetup = remoteConfig.getBoolean("pin_setup_screen_display")
            sharedPreferenceUtils.isPinSetupEnabled = showPinSetup

            val showThemeSelection = remoteConfig.getBoolean("choose_your_theme_screen_display")
            sharedPreferenceUtils.isThemeSelectionEnabled = showThemeSelection
            val libraryAdFrequency = remoteConfig.getString("library_native_ad_after_items")
            if (libraryAdFrequency.isNotEmpty()) {
                sharedPreferenceUtils.libraryNativeAdAfterItems = libraryAdFrequency
            } else {
                sharedPreferenceUtils.libraryNativeAdAfterItems = "2" // Fallback default
            }
            Log.d(TAG_REMOTE, "RemoteConfig: Flags Updated -> Onboard: $showOnboarding, Permission: $showPermission, Name: $showNameWriting, Pin: $showPinSetup, Theme: $showThemeSelection")
            // 2. Fetch and save the Ads JSON logic
            val isIAPPlan = remoteConfig.getBoolean("is_iap_plan_active")
            val jsonKey = if (isIAPPlan) {
                if (BuildConfig.DEBUG) "dairy_ads_iap_debug" else "dairy_ads_iap_release"
            } else {
                if (BuildConfig.DEBUG) "dairy_ads_debug" else "dairy_ads_release"
            }

            val adsJsonString = remoteConfig.getString(jsonKey)

            if (adsJsonString.isNotEmpty()) {
                sharedPreferenceUtils.adsJson = adsJsonString
                Log.d(TAG_REMOTE, "RemoteConfig: Loaded plan key: $jsonKey")
            }

        } catch (ex: Exception) {
            // Ensure you have a reference to your exception recorder or replace with Log.e
            Log.e(TAG_REMOTE, "RemoteConfiguration: updateRemoteValues JSON Error: ${ex.message}")
        }
    }
}