package com.example.easydiarysatti.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
) : ViewModel() {

    private val _mainState = MutableStateFlow<MainState>(MainState.Init)
    val mainState: StateFlow<MainState> = _mainState

    fun setMainState(state: MainState) {
        viewModelScope.launch {
            _mainState.update { state }
        }
    }

    fun saveUserProfileRemote() {
        viewModelScope.launch {

        }
    }

}

sealed interface MainState {
    data object Init : MainState
    data object HomeScreen : MainState
}