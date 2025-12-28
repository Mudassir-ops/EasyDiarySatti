package com.example.easydiarysatti.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    fun savePin(enteredPin: String) {
        viewModelScope.launch {
            sessionManagerRepo.setPin(pin = enteredPin)
        }
    }

    fun getPin(): String? {
        return sessionManagerRepo.getPin()
    }

}