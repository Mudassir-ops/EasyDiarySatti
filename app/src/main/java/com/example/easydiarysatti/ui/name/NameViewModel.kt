package com.example.easydiarysatti.ui.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NameViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {

    fun saveName(name: String) {
        viewModelScope.launch {
            sessionManagerRepo.setProfileName(profilePic = name)
        }
    }

    fun saveEmail(mail: String) {
        viewModelScope.launch {
            sessionManagerRepo.setProfileEmail(email = mail)
        }
    }


    fun getName(): String? {
        return sessionManagerRepo.getprofileName()
    }


    fun getEmail(): String? {
        return sessionManagerRepo.getprofileEmail()
    }

}