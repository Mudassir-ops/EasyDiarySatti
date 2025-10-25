package com.example.easydiarysatti.ui.welcome

import androidx.lifecycle.ViewModel
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel(){

    fun getName(): String? {
        return sessionManagerRepo.getprofileName()
    }

}

