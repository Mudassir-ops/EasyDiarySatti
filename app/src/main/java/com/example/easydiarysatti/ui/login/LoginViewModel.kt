package com.example.easydiarysatti.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Init)
    val loginState: StateFlow<LoginState> = _loginState

    fun verifyPin(enteredPin: String) {
        viewModelScope.launch {
            try {
                val savedPin = sessionManagerRepo.getPin().orEmpty()
                if (savedPin == enteredPin) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Wrong password")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.value = LoginState.Error("Failed to verify PIN")
            }
        }
    }
}

sealed interface LoginState {
    object Init : LoginState
    data object Success : LoginState
    data class Error(val message: String) : LoginState
}
