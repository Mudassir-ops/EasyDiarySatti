package com.example.easydiarysatti.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    private val _loginState = MutableSharedFlow<LoginState>()
    val loginState: SharedFlow<LoginState> = _loginState

    fun verifyPin(enteredPin: String) {
        viewModelScope.launch {
            try {
                val savedPin = sessionManagerRepo.getPin().orEmpty()
                if (savedPin == enteredPin) {
                    _loginState.emit(LoginState.Success)
                } else {
                    _loginState.emit(LoginState.Error("Wrong password"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _loginState.emit(LoginState.Error("Failed to verify PIN"))
            }
        }
    }
}

sealed interface LoginState {
    object Init : LoginState
    data object Success : LoginState
    data class Error(val message: String) : LoginState
}
