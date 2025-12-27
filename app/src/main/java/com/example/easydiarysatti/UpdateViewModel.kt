package com.example.easydiarysatti

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.repo.UpdateManagerWrapper
import com.example.easydiarysatti.data.repo.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
) : ViewModel() {

    private var updateManagerWrapper: UpdateManagerWrapper? = null

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    fun init(activity: AppCompatActivity) {
        if (updateManagerWrapper == null) {
            updateManagerWrapper = UpdateManagerWrapper(activity = activity).also {
                observe(it)
            }
        }
    }

    private fun observe(wrapper: UpdateManagerWrapper) {
        viewModelScope.launch {
            wrapper.installStatus.collect { state -> _updateState.value = state }
        }
    }

    fun checkForUpdates() = updateManagerWrapper?.checkForUpdates()
    fun completeUpdate() = updateManagerWrapper?.completeUpdate()
    fun unregisterListener() = updateManagerWrapper?.unregisterListener()
    fun checkDownloadedOnResume() = updateManagerWrapper?.checkForDownloadedUpdateOnResume()
}