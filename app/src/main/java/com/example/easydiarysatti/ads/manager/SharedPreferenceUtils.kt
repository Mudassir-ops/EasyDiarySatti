package com.example.easydiarysatti.ads.manager

import android.content.SharedPreferences
import jakarta.inject.Inject
import org.json.JSONArray
import org.json.JSONObject


class SharedPreferenceUtils @Inject constructor(private val sharedPreferences: SharedPreferences) {

    // ─── Keys ────────────────────────────────────────────────────────────────
    private val billingRequireKey        = "isAppPurchased"
    private val firstTimeUserKey         = "is_first_time_user"
    private val adsJsonKey               = "ads_json_data"
    private val libraryAdFrequencyKey    = "library_native_ad_after_items"

    // ─── All names that live in iapJson ──────────────────────────────────────
    private val iapItemNames = setOf(
        "onboarding_paywall_config",
        "splash_paywall_config",
        "main_paywall_config",
        "remove_ads_inter_ad_cross_ipu",
        "free_add_note_save_quota",
        "exit_popup_config"
    )

    // ─── JSON storage ────────────────────────────────────────────────────────
    var iaaJson: String
        get() = sharedPreferences.getString("iaa_json_data", "") ?: ""
        set(value) = sharedPreferences.edit().putString("iaa_json_data", value).apply()

    var iapJson: String
        get() = sharedPreferences.getString("iap_json_data", DEFAULT_IAP_JSON) ?: DEFAULT_IAP_JSON
        set(value) = sharedPreferences.edit().putString("iap_json_data", value).apply()

    // ─── Onboarding Flow JSON ─────────────────────────────────────────────────
    /**
     * Stores the full onboarding_flow JSON received from Remote Config.
     * Default is the hardcoded JSON matching the current "case_full" behaviour
     * so the app works correctly before the first Remote Config fetch.
     */
    var onboardingFlowJson: String
        get() = sharedPreferences.getString("onboarding_flow_json", DEFAULT_ONBOARDING_FLOW_JSON)
            ?: DEFAULT_ONBOARDING_FLOW_JSON
        set(value) = sharedPreferences.edit().putString("onboarding_flow_json", value).apply()

    companion object {
        /**
         * Default onboarding flow JSON.
         * Mirrors the JSON provided in the product spec — case_full for first open,
         * case_pin_lock for returning open.
         */
        val DEFAULT_ONBOARDING_FLOW_JSON = """
            {
              "onboarding_flow": {
                "first_open": {
                  "active_case": "case_full",
                  "cases": {
                    "case_full": ["onboarding_slides","permission","name","pin","theme","home"],
                    "case_no_permission": ["onboarding_slides","name","pin","theme","home"],
                    "case_no_name": ["onboarding_slides","permission","pin","theme","home"],
                    "case_no_pin": ["onboarding_slides","permission","name","theme","home"],
                    "case_no_theme": ["onboarding_slides","permission","name","pin","home"],
                    "case_no_permission_no_name": ["onboarding_slides","pin","theme","home"],
                    "case_no_permission_no_pin": ["onboarding_slides","name","theme","home"],
                    "case_no_permission_no_theme": ["onboarding_slides","name","pin","home"],
                    "case_no_name_no_pin": ["onboarding_slides","permission","theme","home"],
                    "case_no_name_no_theme": ["onboarding_slides","permission","pin","home"],
                    "case_no_pin_no_theme": ["onboarding_slides","permission","name","home"],
                    "case_slides_only": ["onboarding_slides","home"],
                    "case_no_onboarding_slides": ["permission","name","pin","theme","home"],
                    "case_minimal": ["home"]
                  }
                },
                "returning_open": {
                  "active_case": "case_pin_lock",
                  "cases": {
                    "case_pin_lock": ["login","home"],
                    "case_no_lock": ["home"]
                  }
                }
              }
            }
        """.trimIndent()

        val DEFAULT_IAP_JSON = """
            {
              "Items": [
                {
                  "name": "onboarding_paywall_config",
                  "splash_onboarding_paywall_screen_display": "true",
                  "splash_onboarding_paywall_screen_plan_type": "annual",
                  "splash_onboarding_paywall_screen_highlighted_plan": "annual",
                  "onboarding_paywall_layout_variant": "Variant A"
                },
                {
                  "name": "splash_paywall_config",
                  "splash_paywall_master_show": "true",
                  "splash_home_paywall_screen_display": "-2",
                  "splash_home_paywall_screen_highlighted_plan": "annual",
                  "splash_home_paywall_screen_plan_type": "annual",
                  "splash_paywall_layout_variant": "Variant B"
                },
                {
                  "name": "main_paywall_config",
                  "main_paywall_screen_highlighted_plan": "annual",
                  "main_paywall_screen_plan_type": "annual"
                },
                {
                  "name": "remove_ads_inter_ad_cross_ipu",
                  "remove_ads_inter_ad_cross_ipu": "2",
                  "remove_ads_popup_layout_variant": "Variant B"
                },
                {
                  "name": "free_add_note_save_quota",
                  "free_add_note_save_quota": "3",
                  "free_add_note_media_quota": "2"
                },
                {
                  "name": "exit_popup_config",
                  "exit_popup_display": "true",
                  "exit_popup_layout_variant": "Variant A"
                }
              ]
            }
        """.trimIndent()

        const val AppOpenAdKey_RESUME_VALUE = "app_open_resume"

        // ── Screen name constants (match the JSON string values exactly) ─────
        const val SCREEN_ONBOARDING_SLIDES = "onboarding_slides"
        const val SCREEN_PERMISSION        = "permission"
        const val SCREEN_NAME              = "name"
        const val SCREEN_PIN               = "pin"
        const val SCREEN_THEME             = "theme"
        const val SCREEN_HOME              = "home"
        const val SCREEN_LOGIN             = "login"
    }

    // ─── Onboarding Flow JSON helpers ─────────────────────────────────────────

    /**
     * Parses [onboardingFlowJson] and returns the active screen sequence for
     * the first-open flow as an ordered list of screen name strings.
     *
     * e.g. ["onboarding_slides", "permission", "name", "pin", "theme", "home"]
     *
     * Falls back to ["onboarding_slides", "home"] on any parse error.
     */
    fun getFirstOpenScreenFlow(): List<String> {
        return getActiveFlowScreens("first_open")
    }

    /**
     * Parses [onboardingFlowJson] and returns the active screen sequence for
     * the returning-open flow.
     *
     * e.g. ["login", "home"]  or  ["home"]
     */
    fun getReturningOpenScreenFlow(): List<String> {
        return getActiveFlowScreens("returning_open")
    }

    private fun getActiveFlowScreens(flowKey: String): List<String> {
        return try {
            val root      = JSONObject(onboardingFlowJson)
            val flowObj   = root.getJSONObject("onboarding_flow").getJSONObject(flowKey)
            val activeCase = flowObj.getString("active_case")
            val cases     = flowObj.getJSONObject("cases")
            val arr       = cases.getJSONArray(activeCase)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            // Safe fallback: onboarding slides → home
            listOf(SCREEN_ONBOARDING_SLIDES, SCREEN_HOME)
        }
    }

    /**
     * Convenience: given the current first-open flow, returns the *next* screen
     * that should be shown after [currentScreen].
     *
     * Returns null if [currentScreen] is the last item or is not found.
     */
    fun nextScreenAfter(currentScreen: String): String? {
        val flow = getFirstOpenScreenFlow()
        val idx  = flow.indexOf(currentScreen)
        return if (idx >= 0 && idx < flow.size - 1) flow[idx + 1] else null
    }

    /**
     * Returns true when [screen] is present in the active first-open flow.
     */
    fun isScreenInFlow(screen: String): Boolean =
        getFirstOpenScreenFlow().contains(screen)

    // ─── Backward-compatible individual flag accessors ────────────────────────
    // These are now derived from the JSON flow so that any existing code that
    // still reads them directly continues to work without modification.

    val isOnboardingEnabled: Boolean
        get() = isScreenInFlow(SCREEN_ONBOARDING_SLIDES)

    val isPermissionEnabled: Boolean
        get() = isScreenInFlow(SCREEN_PERMISSION)

    val isNameWritingEnabled: Boolean
        get() = isScreenInFlow(SCREEN_NAME)

    val isPinSetupEnabled: Boolean
        get() = isScreenInFlow(SCREEN_PIN)

    val isThemeSelectionEnabled: Boolean
        get() = isScreenInFlow(SCREEN_THEME)

    // ─── App state ───────────────────────────────────────────────────────────
    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(billingRequireKey, false)
        set(value) = sharedPreferences.edit().putBoolean(billingRequireKey, value).apply()

    var hasUsedFreeTrial: Boolean
        get()      = sharedPreferences.getBoolean("has_used_free_trial", false)
        set(value) = sharedPreferences.edit().putBoolean("has_used_free_trial", value).apply()

    var isFirstTimeUser: Boolean
        get() = sharedPreferences.getBoolean(firstTimeUserKey, false)
        set(value) = sharedPreferences.edit().putBoolean(firstTimeUserKey, value).apply()

    var splashTimeout: Long
        get() = sharedPreferences.getLong("splash_timeout", 6000L)
        set(value) = sharedPreferences.edit().putLong("splash_timeout", value).apply()

    var isPermissionDone: Boolean
        get() = sharedPreferences.getBoolean("permission_done", false)
        set(value) = sharedPreferences.edit().putBoolean("permission_done", value).apply()

    // ─── Ad frequency ────────────────────────────────────────────────────────
    var libraryNativeAdAfterItems: String
        get() = sharedPreferences.getString(libraryAdFrequencyKey, "2") ?: "2"
        set(value) = sharedPreferences.edit().putString(libraryAdFrequencyKey, value).apply()

    var adsJson: String
        get() = sharedPreferences.getString(adsJsonKey, "") ?: ""
        set(value) = sharedPreferences.edit().putString(adsJsonKey, value).apply()

    var isInternetPopupFtuDone: Boolean
        get() = sharedPreferences.getBoolean("internet_popup_ftu_done", false)
        set(value) = sharedPreferences.edit().putBoolean("internet_popup_ftu_done", value).apply()

    var internetConnectivityDisplay: Long
        get() = sharedPreferences.getLong("internet_connectivity_display", -2L)
        set(value) = sharedPreferences.edit().putLong("internet_connectivity_display", value).apply()

    private var internetConnectivityLastShownAt: Long
        get() = sharedPreferences.getLong("internet_connectivity_last_shown", 0L)
        set(value) = sharedPreferences.edit().putLong("internet_connectivity_last_shown", value).apply()

    var internetConnectivityCounter: Int
        get() = sharedPreferences.getInt("internet_connectivity_counter", 0)
        set(value) = sharedPreferences.edit().putInt("internet_connectivity_counter", value).apply()

    // ─── Splash Paywall counters ──────────────────────────────────────────────
    private val splashLastTimeKey  = "splash_paywall_last_time"
    private val splashCounterKey   = "splash_counter_key"

    var splashPaywallLastTime: Long
        get() = sharedPreferences.getLong(splashLastTimeKey, 0L)
        set(value) = sharedPreferences.edit().putLong(splashLastTimeKey, value).apply()

    var splashPaywallCounter: Int
        get() = sharedPreferences.getInt(splashCounterKey, 0)
        set(value) = sharedPreferences.edit().putInt(splashCounterKey, value).apply()

    fun shouldShowInternetConnectivityPopup(): Boolean {
        return when (val mode = internetConnectivityDisplay) {
            0L  -> false
            -1L -> true
            -2L -> {
                val now      = System.currentTimeMillis()
                val oneDayMs = 24L * 60 * 60 * 1000
                if (now - internetConnectivityLastShownAt > oneDayMs) {
                    internetConnectivityLastShownAt = now
                    true
                } else false
            }
            else -> {
                val n = mode.toInt()
                when {
                    internetConnectivityCounter == 0 -> true
                    internetConnectivityCounter >= n -> {
                        internetConnectivityCounter = 0
                        true
                    }
                    else -> false
                }
            }
        }
    }

    fun recordInternetPopupRemindLater() {
        if (internetConnectivityDisplay > 0L) {
            internetConnectivityCounter++
        }
    }

    fun getRewardedLoadingDialogShow(adName: String): Boolean =
        getAdObject(adName)?.optBoolean("loading_dialog_show", false) ?: false

    fun getRewardedLoadingDialogTime(adName: String): Long {
        val raw = getAdObject(adName)?.optString("loading_dialog_time", "2000") ?: "2000"
        return raw.toLongOrNull() ?: 2000L
    }

    fun shouldShowSplashPaywall(): Boolean {
        if (isAppPurchased) return false
        val splashConfig = getAdObject("splash_paywall_config") ?: return false
        val masterShow = splashConfig.optString("splash_paywall_master_show", "false")
        if (masterShow != "true") return false
        val displayValue = splashConfig.optString("splash_home_paywall_screen_display", "0")
            .toIntOrNull() ?: 0
        return when {
            displayValue == -1 -> true
            displayValue == -2 -> {
                val now = System.currentTimeMillis()
                val oneDayMs = 24L * 60 * 60 * 1000
                if ((now - splashPaywallLastTime) > oneDayMs) {
                    splashPaywallLastTime = now
                    true
                } else false
            }
            displayValue > 0 -> {
                val next = splashPaywallCounter + 1
                if (next >= displayValue) {
                    splashPaywallCounter = 0
                    true
                } else {
                    splashPaywallCounter = next
                    false
                }
            }
            else -> false
        }
    }

    private val interCrossCountKey = "inter_cross_count"

    var interstitialCrossCount: Int
        get() = sharedPreferences.getInt(interCrossCountKey, 0)
        set(value) = sharedPreferences.edit().putInt(interCrossCountKey, value).apply()

    fun shouldShowRemoveAdsPopup(): Boolean {
        if (isAppPurchased) return false
        val config = getAdObject("remove_ads_inter_ad_cross_ipu") ?: return false
        val threshold = config.optString("remove_ads_inter_ad_cross_ipu", "2")
            .toIntOrNull() ?: 2
        val newCount = interstitialCrossCount + 1
        return if (newCount > threshold) {
            interstitialCrossCount = 0
            true
        } else {
            interstitialCrossCount = newCount
            false
        }
    }

    fun getRemoveAdsVariant(): String {
        val obj = getAdObject("remove_ads_inter_ad_cross_ipu")
        return when (obj?.optString("remove_ads_popup_layout_variant")) {
            "Variant A", "1" -> "Variant A"
            "Variant B", "2" -> "Variant B"
            else -> "Variant A"
        }
    }

    // ─── Rewarded gate quotas ─────────────────────────────────────────────────
    private val saveNoteUsageKey  = "save_note_usage_count"
    private val mediaNoteUsageKey = "media_note_usage_count"

    var saveNoteUsageCount: Int
        get() = sharedPreferences.getInt(saveNoteUsageKey, 0)
        set(value) = sharedPreferences.edit().putInt(saveNoteUsageKey, value).apply()

    var mediaNoteUsageCount: Int
        get() = sharedPreferences.getInt(mediaNoteUsageKey, 0)
        set(value) = sharedPreferences.edit().putInt(mediaNoteUsageKey, value).apply()

    fun getSaveNoteQuota(): Int {
        val config = getAdObject("free_add_note_save_quota") ?: return 3
        return config.optString("free_add_note_save_quota", "3").toIntOrNull() ?: 3
    }

    fun shouldShowRewardedForSave(): Boolean {
        if (isAppPurchased) return false
        val config = getAdObject("free_add_note_save_quota") ?: return false
        val quota = getSaveNoteQuota()
        return saveNoteUsageCount >= quota
    }

    fun shouldShowRewardedForMedia(): Boolean {
        if (isAppPurchased) return false
        val config = getAdObject("free_add_note_save_quota") ?: return false
        val quota = config.optString("free_add_note_media_quota", "2").toIntOrNull() ?: 2
        return mediaNoteUsageCount >= quota
    }

    // ─── Exit Popup helpers ───────────────────────────────────────────────────
    fun shouldShowExitPopup(): Boolean {
        val config = getAdObject("exit_popup_config") ?: return true
        return config.optString("exit_popup_display", "true").equals("true", ignoreCase = true)
    }

    fun getExitPopupVariant(): String {
        val config = getAdObject("exit_popup_config") ?: return "Variant A"
        return when (val v = config.optString("exit_popup_layout_variant", "Variant A").trim()) {
            "Variant A", "Variant B", "Variant C" -> v
            else -> "Variant A"
        }
    }

    // ─── App Open Resume frequency helpers ───────────────────────────────────
    private var appOpenResumeLastShownAt: Long
        get()      = sharedPreferences.getLong("app_open_resume_last_shown", 0L)
        set(value) = sharedPreferences.edit().putLong("app_open_resume_last_shown", value).apply()

    var appOpenResumeSessionCounter: Int
        get()      = sharedPreferences.getInt("app_open_resume_session_counter", 0)
        set(value) = sharedPreferences.edit().putInt("app_open_resume_session_counter", value).apply()

    fun getAppOpenResumeFrequency(): Int {
        val obj = getAdObject(AppOpenAdKey_RESUME_VALUE) ?: return -1
        return obj.optString("app_open_resume_frequency", "-1").toIntOrNull() ?: -1
    }

    fun shouldShowAppOpenOnResume(): Boolean {
        if (isAppPurchased) return false
        return when (val freq = getAppOpenResumeFrequency()) {
            0    -> false
            -1   -> true
            -2   -> {
                val now      = System.currentTimeMillis()
                val oneDayMs = 24L * 60 * 60 * 1000
                if (now - appOpenResumeLastShownAt > oneDayMs) {
                    appOpenResumeLastShownAt = now
                    true
                } else false
            }
            else -> {
                val next = appOpenResumeSessionCounter + 1
                if (next >= freq) {
                    appOpenResumeSessionCounter = 0
                    true
                } else {
                    appOpenResumeSessionCounter = next
                    false
                }
            }
        }
    }

    private fun getAdObject(adName: String): JSONObject? {
        return try {
            val jsonSource = if (iapItemNames.contains(adName)) iapJson else iaaJson
            if (jsonSource.isEmpty()) return null
            val root  = JSONObject(jsonSource)
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

    // ─── Generic ad attribute getters ────────────────────────────────────────
    fun getAdId(adName: String): String =
        getAdObject(adName)?.optString("id", "") ?: ""

    fun getAdShowStatus(adName: String): Boolean {
        val obj = getAdObject(adName) ?: return iapItemNames.contains(adName).not()
        return obj.optBoolean("show", true)
    }

    fun getAdDialogStatus(adName: String): Boolean =
        getAdObject(adName)?.optBoolean("dialog_show", false) ?: false

    fun getAdDialogTime(adName: String): Long {
        val time = getAdObject(adName)?.optString("dialog_time", "1000") ?: "1000"
        return time.toLongOrNull() ?: 1000L
    }

    fun getAdExtraData(adName: String, key: String): String =
        getAdObject(adName)?.optString(key, "") ?: ""
}