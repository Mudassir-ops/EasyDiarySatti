package com.example.easydiarysatti.ads.manager

import android.content.SharedPreferences
import jakarta.inject.Inject
import org.json.JSONObject


class SharedPreferenceUtils @Inject constructor(private val sharedPreferences: SharedPreferences) {

    private val billingRequireKey = "isAppPurchased"
    private val firstTimeUserKey = "is_first_time_user"
    private val adsJsonKey = "ads_json_data"
    private val onboardingDisplayKey = "onboarding_slides_display"
    private val permissionDisplayKey = "permission_screen_display"
    private val nameDisplayKey = "start_writing_display"
    private val pinSetupScreenDisplay = "pin_setup_screen_display"
    private val themeScreenDisplay = "choose_your_theme_screen_display"
    private val libraryAdFrequencyKey = "library_native_ad_after_items"
    /* ---------- Billing & Subscription ---------- */

    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(billingRequireKey, false)
        set(value) = sharedPreferences.edit().putBoolean(billingRequireKey, value).apply()

    /* ---------- User Logic (New vs Returning) ---------- */
// Add this inside SharedPreferenceUtils class
    var splashTimeout: Long
        get() = sharedPreferences.getLong("splash_timeout", 6000L) // Default 6 seconds
        set(value) = sharedPreferences.edit().putLong("splash_timeout", value).apply()
    var isFirstTimeUser: Boolean
        get() = sharedPreferences.getBoolean(firstTimeUserKey, true)
        set(value) = sharedPreferences.edit().putBoolean(firstTimeUserKey, value).apply()

    // Key = library_native_ad_after_items. Default: "2"
    var libraryNativeAdAfterItems: String
        get() = sharedPreferences.getString(libraryAdFrequencyKey, "2") ?: "2"
        set(value) = sharedPreferences.edit().putString(libraryAdFrequencyKey, value).apply()

    /* ---------- Remote Config JSON Storage ---------- */

    // This stores the raw JSON string fetched from Firebase Remote Config
    var adsJson: String
        get() = sharedPreferences.getString(adsJsonKey, "") ?: ""
        set(value) = sharedPreferences.edit().putString(adsJsonKey, value).apply()


    var isOnboardingEnabled: Boolean
        get() = sharedPreferences.getBoolean(onboardingDisplayKey, true) // Default to true
        set(value) = sharedPreferences.edit().putBoolean(onboardingDisplayKey, value).apply()
    var isPermissionEnabled: Boolean
        get() = sharedPreferences.getBoolean(permissionDisplayKey, true)
        set(value) = sharedPreferences.edit().putBoolean(permissionDisplayKey, value).apply()

    var isNameWritingEnabled: Boolean
        get() = sharedPreferences.getBoolean(nameDisplayKey, true)
        set(value) = sharedPreferences.edit().putBoolean(nameDisplayKey, value).apply()

    var isPinSetupEnabled: Boolean
        get() = sharedPreferences.getBoolean(pinSetupScreenDisplay, true)
        set(value) = sharedPreferences.edit().putBoolean(pinSetupScreenDisplay, value).apply()

    var isThemeSelectionEnabled: Boolean
        get() = sharedPreferences.getBoolean(themeScreenDisplay, true)
        set(value) = sharedPreferences.edit().putBoolean(themeScreenDisplay, value).apply()
    /**
     * Finds a specific ad object in the "Items" array by its "name" field.
     */
    private fun getAdObject(adName: String): JSONObject? {
        return try {
            if (adsJson.isEmpty()) return null
            val root = JSONObject(adsJson)
            val items = root.getJSONArray("Items")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.getString("name") == adName) return item
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /* ---------- Getters for Ad Attributes ---------- */

    fun getAdId(adName: String): String {
        return getAdObject(adName)?.optString("id", "") ?: ""
    }

    fun getAdShowStatus(adName: String): Boolean {
        return getAdObject(adName)?.optBoolean("show", false) ?: false
    }

    /**
     * Checks if the "Loading Ad" dialog should be shown.
     */
    fun getAdDialogStatus(adName: String): Boolean {
        return getAdObject(adName)?.optBoolean("dialog_show", false) ?: false
    }

    /**
     * Gets the duration for the Loading Dialog. Defaults to 1000ms.
     */
    fun getAdDialogTime(adName: String): Long {
        val time = getAdObject(adName)?.optString("dialog_time", "1000") ?: "1000"
        return time.toLongOrNull() ?: 1000L
    }

    /**
     * Fetches custom extra data from the JSON (e.g., "ad_type_choice").
     */
    fun getAdExtraData(adName: String, key: String): String {
        return getAdObject(adName)?.optString(key, "") ?: ""
    }
}