package com.example.easydiarysatti.data.repo

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class UpdateManagerWrapper @Inject constructor(
    private val activity: AppCompatActivity
) {
    private val updateManager = AppUpdateManagerFactory.create(activity.application)

    private val _installStatus = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val installStatus: StateFlow<UpdateState> = _installStatus

    private lateinit var updateLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var installStateListener: InstallStateUpdatedListener? = null
    private var listenerRegistered = false

    init {
        registerUpdateLauncher()
    }

    private fun registerUpdateLauncher() {
        updateLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    _installStatus.value = UpdateState.Completed
                }

                Activity.RESULT_CANCELED -> {
                    _installStatus.value = UpdateState.Cancelled
                }

                else -> {
                    Log.d("Update", "Update resultCode: ${result.resultCode}")
                    _installStatus.value = UpdateState.Failed
                }
            }
        }
    }

    fun checkForUpdates() {
        _installStatus.value = UpdateState.Checking

        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            val staleness = info.clientVersionStalenessDays()
            val priority = info.updatePriority()
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

            if (isUpdateAvailable) {
                when {
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) && priority >= 4 -> {
                        _installStatus.value =
                            UpdateState.UpdateAvailable(true, staleness, priority)
                        startUpdate(info, AppUpdateType.IMMEDIATE)
                    }

                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        _installStatus.value =
                            UpdateState.UpdateAvailable(false, staleness, priority)
                        startUpdate(info, AppUpdateType.FLEXIBLE)
                        observeFlexibleUpdates()
                    }

                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        _installStatus.value = UpdateState.Downloaded
                    }

                    else -> {
                        _installStatus.value = UpdateState.Idle
                    }
                }
            }
        }.addOnFailureListener {
            _installStatus.value = UpdateState.Failed
            Log.d("In-App-Update-Log", "failed message ${it.message}")
        }
    }

    private fun startUpdate(info: AppUpdateInfo, type: Int) {
        val options = AppUpdateOptions.newBuilder(type)
            .setAllowAssetPackDeletion(true)
            .build()
        try {
            if (type == AppUpdateType.IMMEDIATE) {
                updateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    options
                )
            } else {
                updateManager.startUpdateFlow(
                    info,
                    activity,
                    options
                )
            }
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
            _installStatus.value = UpdateState.Failed
        } catch (e: Exception) {
            e.printStackTrace()
            _installStatus.value = UpdateState.Failed
        }
    }


    private fun observeFlexibleUpdates() {
        if (listenerRegistered) return

        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> _installStatus.value = UpdateState.Downloaded
                InstallStatus.INSTALLING -> _installStatus.value = UpdateState.Installing
                InstallStatus.INSTALLED -> _installStatus.value = UpdateState.Completed
                InstallStatus.DOWNLOADING -> {
                    val bytesDownloaded = state.bytesDownloaded()
                    val totalBytesToDownload = state.totalBytesToDownload()

                    _installStatus.value = UpdateState.Downloading(
                        totalBytes = totalBytesToDownload,
                        bytesDownloaded = bytesDownloaded
                    )
                }

                else -> {} // no-op
            }
        }
        installStateListener?.let {
            updateManager.registerListener(it)
            listenerRegistered = true
        }
    }

    fun checkForDownloadedUpdateOnResume() {
        updateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    updateManager.startUpdateFlowForResult(
                        info,
                        updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                    )
                }
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _installStatus.value = UpdateState.Downloaded
                }
            }
    }

    fun completeUpdate() {
        updateManager.completeUpdate()
    }

    fun unregisterListener() {
        installStateListener?.let {
            updateManager.unregisterListener(it)
            listenerRegistered = false
            installStateListener = null
        }
    }
}