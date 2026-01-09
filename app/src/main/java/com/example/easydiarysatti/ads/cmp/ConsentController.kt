package com.example.easydiarysatti.ads.cmp

import android.app.Activity
import android.util.Log
import com.example.easydiarysatti.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.ConsentStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentController(private val activity: Activity) {

    private var consentInformation: ConsentInformation? = null
    private var consentCallback: ConsentCallback? = null
    private var consentForm: ConsentForm? = null

    private val TAG = "ConsentController"

    val canRequestAds: Boolean get() = consentInformation?.canRequestAds() == true

    interface ConsentCallback {
        fun onAdsLoad(canRequestAds: Boolean)
        fun onPolicyStatus(isRequired: Boolean)
        fun onConsentFormLoaded() {}
        fun onConsentFormDismissed() {}
    }

    fun initConsent(deviceId: String = "", callback: ConsentCallback?) {
        this.consentCallback = callback
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // --- FOR TESTING ONLY ---
        // Uncomment the line below to reset the consent state and force the form to show every time
        // consentInformation?.reset()

        val params = if (BuildConfig.DEBUG) {
            // Debug settings for developers
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId(deviceId)
                .build()

            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            // Production settings for real users
            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
        }

        consentInformation?.requestConsentInfoUpdate(activity, params, {
            val isFormAvailable = consentInformation?.isConsentFormAvailable == true

            if (isFormAvailable) {
                handleConsentStatus()
            } else {
                consentCallback?.onAdsLoad(canRequestAds)
            }

            val isPolicyRequired = consentInformation?.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            consentCallback?.onPolicyStatus(isPolicyRequired)

        }, { error ->
            Log.e(TAG, "initializationError: ${error.message}")
            consentCallback?.onAdsLoad(canRequestAds)
        })
    }

    private fun handleConsentStatus() {
        when (consentInformation?.consentStatus) {
            ConsentStatus.REQUIRED -> loadConsentForm()
            ConsentStatus.OBTAINED, ConsentStatus.NOT_REQUIRED -> {
                consentCallback?.onAdsLoad(canRequestAds)
            }
            else -> consentCallback?.onAdsLoad(canRequestAds)
        }
    }

    private fun loadConsentForm() {
        UserMessagingPlatform.loadConsentForm(activity, { form ->
            this.consentForm = form
            showConsentForm()
        }, { error ->
            Log.e(TAG, "Consent Form Load Fail: ${error.message}")
            consentCallback?.onAdsLoad(canRequestAds)
        })
    }

    fun showConsentForm() {
        consentForm?.show(activity) { error ->
            if (error != null) {
                Log.e(TAG, "Form Show Error: ${error.message}")
            }
            consentCallback?.onConsentFormDismissed()
            consentCallback?.onAdsLoad(canRequestAds)
        }
    }
}