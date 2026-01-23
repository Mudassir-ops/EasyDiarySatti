package com.example.easydiarysatti.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashState {
    data object Idle : SplashState
    data object ShowAd : SplashState
    data object NavigateToOnboarding : SplashState
    data object NavigateToLogin : SplashState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    // 1. Change to replay = 0 to prevent old events from sticking
    // We will handle the "stuck" issue using a different method below
    private val _state = MutableSharedFlow<SplashState>(replay = 0)
    val state: SharedFlow<SplashState> = _state

    private var navigationJob: Job? = null
    private var isNavigated = false

    fun startLogic(hasInternet: Boolean) {
        // Reset navigation flag for the new session
        isNavigated = false

        navigationJob = viewModelScope.launch {
            // Force a small delay so the user definitely sees the splash
            delay(500L)

            if (!hasInternet) {
                delay(2000L)
                decideNavigation()
            } else {
                _state.emit(SplashState.ShowAd)
                // Safety timeout
                delay(12000L)
                decideNavigation()
            }
        }
    }
    fun onAdFinished() {
        // Stop the safety timer immediately
        navigationJob?.cancel()
        decideNavigation()
    }

    private fun decideNavigation() {
        if (isNavigated) return
        isNavigated = true

        viewModelScope.launch {
            if (sessionManagerRepo.isOnBoardingDoneOnce() != true) {
                _state.emit(SplashState.NavigateToOnboarding)
            } else {
                _state.emit(SplashState.NavigateToLogin)
            }
        }
    }
}