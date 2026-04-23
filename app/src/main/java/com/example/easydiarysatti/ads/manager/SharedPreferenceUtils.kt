package com.example.easydiarysatti.ads.manager

import android.content.SharedPreferences
import jakarta.inject.Inject
import org.json.JSONObject


class SharedPreferenceUtils @Inject constructor(private val sharedPreferences: SharedPreferences) {

    // ─── Keys ────────────────────────────────────────────────────────────────
    private val billingRequireKey        = "isAppPurchased"
    private val firstTimeUserKey         = "is_first_time_user"
    private val adsJsonKey               = "ads_json_data"
    private val onboardingDisplayKey     = "onboarding_slides_display"
    private val permissionDisplayKey     = "permission_screen_display"
    private val nameDisplayKey           = "start_writing_display"
    private val pinSetupScreenDisplay    = "pin_setup_screen_display"
    private val themeScreenDisplay       = "choose_your_theme_screen_display"
    private val libraryAdFrequencyKey    = "library_native_ad_after_items"

    // ─── All names that live in iapJson ──────────────────────────────────────
    private val iapItemNames = setOf(
        "onboarding_paywall_config",
        "splash_paywall_config",
        "main_paywall_config",
        "remove_ads_inter_ad_cross_ipu",
        "free_add_note_save_quota",
        "exit_popup_config"           // ← NEW
    )

    // ─── JSON storage ────────────────────────────────────────────────────────
    var iaaJson: String
        get() = sharedPreferences.getString("iaa_json_data", "") ?: ""
        set(value) = sharedPreferences.edit().putString("iaa_json_data", value).apply()

    var iapJson: String
        get() = sharedPreferences.getString("iap_json_data", DEFAULT_IAP_JSON) ?: DEFAULT_IAP_JSON
        set(value) = sharedPreferences.edit().putString("iap_json_data", value).apply()

    companion object {
        /**
         * Default IAP JSON — includes exit_popup_config (Variant A, enabled).
         * Keep in sync with Firebase Remote Config keys dairy_ads_iap_debug /
         * dairy_ads_iap_release.
         */
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

        // Keep the string in one place so AppOpenAdsConfig can reference it
        // without importing AppOpenAdKey (which is in the ads module).
        const val AppOpenAdKey_RESUME_VALUE = "app_open_resume"
    }

    // ─── App state ───────────────────────────────────────────────────────────
    var isAppPurchased: Boolean
        get() = sharedPreferences.getBoolean(billingRequireKey, false)
        set(value) = sharedPreferences.edit().putBoolean(billingRequireKey, value).apply()

    var isFirstTimeUser: Boolean
        get() = sharedPreferences.getBoolean(firstTimeUserKey, false)
        set(value) = sharedPreferences.edit().putBoolean(firstTimeUserKey, value).apply()

    var splashTimeout: Long
        get() = sharedPreferences.getLong("splash_timeout", 6000L)
        set(value) = sharedPreferences.edit().putLong("splash_timeout", value).apply()

    // ─── Onboarding screen flags (from Remote Config booleans) ───────────────
    var isOnboardingEnabled: Boolean
        get() = sharedPreferences.getBoolean(onboardingDisplayKey, true)
        set(value) = sharedPreferences.edit().putBoolean(onboardingDisplayKey, value).apply()

    var isPermissionEnabled: Boolean
        get() = sharedPreferences.getBoolean(permissionDisplayKey, true)
        set(value) = sharedPreferences.edit().putBoolean(permissionDisplayKey, value).apply()

    var isPermissionDone: Boolean
        get() = sharedPreferences.getBoolean("permission_done", false)
        set(value) = sharedPreferences.edit().putBoolean("permission_done", value).apply()

    var isNameWritingEnabled: Boolean
        get() = sharedPreferences.getBoolean(nameDisplayKey, true)
        set(value) = sharedPreferences.edit().putBoolean(nameDisplayKey, value).apply()

    var isPinSetupEnabled: Boolean
        get() = sharedPreferences.getBoolean(pinSetupScreenDisplay, true)
        set(value) = sharedPreferences.edit().putBoolean(pinSetupScreenDisplay, value).apply()

    var isThemeSelectionEnabled: Boolean
        get() = sharedPreferences.getBoolean(themeScreenDisplay, true)
        set(value) = sharedPreferences.edit().putBoolean(themeScreenDisplay, value).apply()

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

    /** Raw Long stored from Remote Config (getLong). Defaults to -2. */
    var internetConnectivityDisplay: Long
        get() = sharedPreferences.getLong("internet_connectivity_display", -2L)
        set(value) = sharedPreferences.edit().putLong("internet_connectivity_display", value).apply()

    /** Epoch-ms of the last time the popup was shown (24-hour mode only). */
    private var internetConnectivityLastShownAt: Long
        get() = sharedPreferences.getLong("internet_connectivity_last_shown", 0L)
        set(value) = sharedPreferences.edit().putLong("internet_connectivity_last_shown", value).apply()

    /**
     * Persisted count of "Remind Me Later" taps since the last popup show.
     * Used in counter mode (N ≥ 1) only. Resets to 0 after the popup fires.
     */
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
            0L  -> false   // disabled
            -1L -> true    // every trigger — no state update
            -2L -> {       // once per 24 hours
                val now      = System.currentTimeMillis()
                val oneDayMs = 24L * 60 * 60 * 1000
                if (now - internetConnectivityLastShownAt > oneDayMs) {
                    internetConnectivityLastShownAt = now
                    true
                } else {
                    false
                }
            }
            else -> {      // counter-based (mode >= 1)
                val n = mode.toInt()
                when {
                    internetConnectivityCounter == 0 -> true          // first trigger
                    internetConnectivityCounter >= n -> {              // threshold reached
                        internetConnectivityCounter = 0
                        true
                    }
                    else -> false                                      // still counting
                }
            }
        }
    }

    /**
     * Call when the user taps "Remind Me Later".
     * Increments the persisted counter used in counter mode (N ≥ 1).
     * No-op for -2 (24-hour) and -1 (always) modes.
     */
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

    // ─── Remove Ads cross-button counter ─────────────────────────────────────
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

    // ─── Exit Popup helpers (NEW) ─────────────────────────────────────────────
    /**
     * Returns true when the exit popup should be shown.
     * Reads `exit_popup_config` → `exit_popup_display` from iapJson.
     * Default: true (popup visible until RC says otherwise).
     */
    fun shouldShowExitPopup(): Boolean {
        val config = getAdObject("exit_popup_config") ?: return true
        return config.optString("exit_popup_display", "true").equals("true", ignoreCase = true)
    }

    /**
     * Returns the layout variant string for the exit popup.
     *
     * Firebase value → ExitPopupDialog.Variant mapping:
     *   "Variant A" → SIMPLE
     *   "Variant B" → WITH_RATING
     *   "Variant C" → WITH_NATIVE
     *
     * Falls back to "Variant A" (SIMPLE) if the key is absent or unrecognised.
     */
    fun getExitPopupVariant(): String {
        val config = getAdObject("exit_popup_config") ?: return "Variant A"
        return when (val v = config.optString("exit_popup_layout_variant", "Variant A").trim()) {
            "Variant A", "Variant B", "Variant C" -> v
            else -> "Variant A"
        }
    }

    // ─── App Open Resume frequency helpers ───────────────────────────────────
    //
    // Remote Config key (inside iaaJson, item name "app_open_resume"):
    //   "app_open_resume_frequency": "<value>"
    //
    // Value semantics:
    //   "-1"        → show on every resume (default / always-on)
    //   "-2"        → show at most once per 24 hours
    //    "0"        → disabled — never show
    //   "1","2","N" → session-based: show on every Nth resume session
    //                 e.g. "1" = every session, "2" = every 2nd session
    //
    // A "session" is one foreground→background→foreground cycle detected by
    // ProcessLifecycleOwner in EasyDiaryApplication.
    // ─────────────────────────────────────────────────────────────────────────

    /** Persisted epoch-ms of the last time the resume app-open was shown. */
    private var appOpenResumeLastShownAt: Long
        get()      = sharedPreferences.getLong("app_open_resume_last_shown", 0L)
        set(value) = sharedPreferences.edit().putLong("app_open_resume_last_shown", value).apply()

    /**
     * Counter that increments on every qualifying resume and resets when the
     * ad is shown (session-based mode only).
     */
    var appOpenResumeSessionCounter: Int
        get()      = sharedPreferences.getInt("app_open_resume_session_counter", 0)
        set(value) = sharedPreferences.edit().putInt("app_open_resume_session_counter", value).apply()

    /**
     * Reads "app_open_resume_frequency" from the iaaJson item "app_open_resume".
     *
     * Returns the raw integer value:
     *   -1  → always
     *   -2  → once per 24 h
     *    0  → off
     *   N>0 → every Nth session
     *
     * Falls back to -1 (always) when the key is absent so existing behaviour
     * is preserved before the first Remote Config fetch.
     */
    fun getAppOpenResumeFrequency(): Int {
        val obj = getAdObject(AppOpenAdKey_RESUME_VALUE) ?: return -1
        return obj.optString("app_open_resume_frequency", "-1").toIntOrNull() ?: -1
    }

    /**
     * The single decision point called by EasyDiaryApplication.showAdOnResume().
     *
     * Returns true when the resume app-open ad should be shown NOW according
     * to the configured frequency value.  Side-effects (updating the last-shown
     * timestamp and session counter) are applied atomically here so that
     * callers never need to know the internals.
     *
     * Frequency semantics:
     *   0   → always false (ad disabled)
     *  -1   → always true  (every resume)
     *  -2   → true only if more than 24 h have elapsed since last show
     *  N>0  → increment counter; true when counter reaches N, then reset to 0
     */
    fun shouldShowAppOpenOnResume(): Boolean {
        if (isAppPurchased) return false

        return when (val freq = getAppOpenResumeFrequency()) {
            0    -> false   // disabled
            -1   -> true    // every resume — no state update needed
            -2   -> {       // once per 24 hours
                val now       = System.currentTimeMillis()
                val oneDayMs  = 24L * 60 * 60 * 1000
                if (now - appOpenResumeLastShownAt > oneDayMs) {
                    appOpenResumeLastShownAt = now
                    true
                } else {
                    false
                }
            }
            else -> {       // session-based (freq >= 1)
                val next = appOpenResumeSessionCounter + 1
                if (next >= freq) {
                    appOpenResumeSessionCounter = 0   // reset after firing
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