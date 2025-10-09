package com.example.easydiarysatti.ui.changepasswordkey

import androidx.lifecycle.ViewModel
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {


    fun getBgTheme(): Int? {
        return sessionManagerRepo.getBgTheme()
    }
}