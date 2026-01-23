package com.example.easydiarysatti.ads.natives.domain.useCases

import android.content.Context
import android.util.Log
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.data.entities.ItemNativeAd
import com.example.easydiarysatti.ads.natives.data.repositories.RepositoryNativeImpl
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.utils.Constants.TAG_ADS
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class UseCaseNative @Inject constructor(
    private val repositoryNativeImpl: RepositoryNativeImpl,
    private val sharedPreferenceUtils: SharedPreferenceUtils,
    private val internetManager: InternetManager,
    @ApplicationContext private val context: Context // Add @ApplicationContext here
) {

    @Volatile
    private var isAdLoading = false

    private fun checkRemoteConfig(nativeAdKey: NativeAdKey): Boolean {
        return when (nativeAdKey) {
            NativeAdKey.LOGIN -> sharedPreferenceUtils.rcNativeLogin != 0
            NativeAdKey.PERMISSION -> sharedPreferenceUtils.rcNativePermission != 0
            NativeAdKey.NAME -> sharedPreferenceUtils.rcNativeName != 0
            NativeAdKey.SIGNUP -> sharedPreferenceUtils.rcNativeSignup != 0
            NativeAdKey.LANGUAGE -> sharedPreferenceUtils.rcNativeLanguage != 0
            NativeAdKey.HOME -> sharedPreferenceUtils.rcNativeHome != 0
            NativeAdKey.LIBRARY -> sharedPreferenceUtils.rcNativeLibrary != 0
            NativeAdKey.CALENDAR -> sharedPreferenceUtils.rcNativeCalendar != 0
            NativeAdKey.PROFILE -> sharedPreferenceUtils.rcNativeProfile != 0
            NativeAdKey.TAGS -> sharedPreferenceUtils.rcNativeTags != 0
            NativeAdKey.EDIT_TAG -> sharedPreferenceUtils.rcNativeEditTag != 0
            NativeAdKey.REMINDER -> sharedPreferenceUtils.rcNativeReminder != 0
            NativeAdKey.CHANGE_PASSWORD -> sharedPreferenceUtils.rcNativeChangePassword != 0

        }
    }

    private fun getAdId(nativeAdKey: NativeAdKey): String {
        val resId = when (nativeAdKey) {
            NativeAdKey.LOGIN -> R.string.admob_native_login
            NativeAdKey.PERMISSION -> R.string.admob_native_permission
            NativeAdKey.NAME -> R.string.admob_native_name
            NativeAdKey.SIGNUP -> R.string.admob_native_signup
            NativeAdKey.LANGUAGE -> R.string.admob_native_language
            NativeAdKey.HOME -> R.string.admob_native_home
            NativeAdKey.LIBRARY -> R.string.admob_native_library
            NativeAdKey.CALENDAR -> R.string.admob_native_calendar
            NativeAdKey.PROFILE -> R.string.admob_native_profile
            NativeAdKey.TAGS -> R.string.admob_native_tags
            NativeAdKey.EDIT_TAG -> R.string.admob_native_edit_tag
            NativeAdKey.REMINDER -> R.string.admob_native_remiander
            NativeAdKey.CHANGE_PASSWORD -> R.string.admob_native_change_password
        }
        return context.getString(resId).trim()
    }

    fun loadNativeAd(nativeAdKey: NativeAdKey, callback: (ItemNativeAd?) -> Unit) {
        validateAndLoadAd(nativeAdKey, callback) { adId ->
            isAdLoading = true
            repositoryNativeImpl.fetchNativeAd(adKey = nativeAdKey.value, adId = adId) {
                isAdLoading = false
                callback.invoke(it)
            }
        }
    }

    private fun validateAndLoadAd(
        nativeAdKey: NativeAdKey,
        callback: (ItemNativeAd?) -> Unit,
        loadAdAction: (adId: String) -> Unit
    ) {
        val isRemoteEnable = checkRemoteConfig(nativeAdKey)
        val adId = getAdId(nativeAdKey)

        when {
            sharedPreferenceUtils.isAppPurchased -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Premium user")
                callback.invoke(null)
            }

            isRemoteEnable.not() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Remote config is off")
                callback.invoke(null)
            }

            internetManager.isInternetConnected.not() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Internet is not connected")
                callback.invoke(null)
            }

            adId.isEmpty() -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Ad id is empty")
                callback.invoke(null)
            }

            isAdLoading -> {
                Log.e(TAG_ADS, "${nativeAdKey.value} -> loadNative: Ad is already loading")
                //callback.invoke(null)
            }

            else -> {
                loadAdAction(adId)
            }
        }
    }

    fun destroyNative(nativeAdKey: NativeAdKey): Boolean {
        val isDestroyed = repositoryNativeImpl.destroyNative(nativeAdKey.value)
        if (isDestroyed)
            Log.e(TAG_ADS, "${nativeAdKey.value} -> destroyNative: destroyed")
        return isDestroyed
    }
}