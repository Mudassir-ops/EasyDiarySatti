package com.example.easydiarysatti.ads.firebase

import android.util.Log
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
        // Set minimum fetch interval to 0 for real-time updates Satti
        val configSettings = remoteConfigSettings {
            setFetchTimeoutInSeconds(10)
            setMinimumFetchIntervalInSeconds(0L)
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun checkRemoteConfig(fetchCallback: (Boolean) -> Unit) {
        var callback: ((Boolean) -> Unit)? = fetchCallback
        if (!internetManager.isInternetConnected) {
            Log.e(TAG_REMOTE, "RemoteConfiguration: checkRemoteConfig: No Internet Found")
            fetchCallback.invoke(false)
            callback = null
        }

        addLiveUpdateListener()
        fetchRemoteValues(callback)
    }

    private fun addLiveUpdateListener() {
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(
                    TAG_REMOTE,
                    "RemoteConfiguration: addLiveUpdateListener: onUpdate: Fetched Successfully"
                )
                remoteConfig.activate().addOnSuccessListener { isSuccessful ->
                    if (isSuccessful) updateRemoteValues()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG_REMOTE, "RemoteConfiguration: addLiveUpdateListener: Error: ", error)
            }
        })
    }

    private fun fetchRemoteValues(callback: ((Boolean) -> Unit)?) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            when (task.isSuccessful) {
                true -> updateRemoteValues()
                false -> Log.e(
                    TAG_REMOTE, "RemoteConfiguration: fetchRemoteValues: Failure: ", task.exception
                )
            }
            callback?.invoke(task.isSuccessful)
        }
    }

    private fun updateRemoteValues() {
        sharedPreferenceUtils.apply {
            try {
                rcAppOpen = remoteConfig[appOpen].asLong().toInt()
                rcBannerHome = remoteConfig[bannerHome].asLong().toInt()
                rcNativeLogin = remoteConfig[nativeLogin].asLong().toInt()
                rcInterFeatureSaveNote = remoteConfig[interSaveNoteFeature].asLong().toInt()
                rcRewardedImageAddFeature =
                    remoteConfig[rewardedImagesOnNoteFeature].asLong().toInt()
                Log.i(
                    TAG_REMOTE,
                    "RemoteConfiguration: rcAppOpen -> ${remoteConfig[appOpen].asLong().toInt()}"
                )
                Log.i(
                    TAG_REMOTE,
                    "RemoteConfiguration: rcBannerHome -> ${
                        remoteConfig[bannerHome].asLong().toInt()
                    }"
                )
                Log.i(
                    TAG_REMOTE,
                    "RemoteConfiguration: interSaveNoteFeature -> ${
                        remoteConfig[interSaveNoteFeature].asLong().toInt()
                    }"
                )
                Log.i(
                    TAG_REMOTE,
                    "RemoteConfiguration: rewardedImagesOnNoteFeature -> ${
                        remoteConfig[rewardedImagesOnNoteFeature].asLong().toInt()
                    }"
                )
            } catch (ex: Exception) {
                ex.recordException("RemoteConfiguration: updateRemoteValues")
            }
        }

        Log.d(TAG_REMOTE, "RemoteConfiguration: updateRemoteValues: Fetched Successfully")
    }

}