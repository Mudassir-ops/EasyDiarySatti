package com.example.easydiarysatti.ui.onboarding

import androidx.lifecycle.ViewModel
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    fun isOnBoardingCompleted(): Boolean {
        return sessionManagerRepo.isOnBoardingDoneOnce() == true
    }

    fun shouldRequireLogin(): Boolean {
        return sessionManagerRepo.shouldRequireLogin()
    }

    fun clearLogin() {
        sessionManagerRepo.clearRequireLogin()
    }

}

