package com.example.easydiarysatti.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.data.mapper.toRoomEntity
import com.example.easydiarysatti.domain.model.FirebaseNote
import com.example.easydiarysatti.domain.model.FirebaseProfile
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.domain.repo.FirebaseRemoteDataSync
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.orEmpty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo,
    private val firebaseRemoteDataSync: FirebaseRemoteDataSync,
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    fun isOnBoardingCompleted(): Boolean {
        return sessionManagerRepo.isOnBoardingDoneOnce() == true
    }

    fun shouldRequireLogin(): Boolean {
        return sessionManagerRepo.shouldRequireLogin()
    }

    fun getCameraCall(): Boolean {
        return sessionManagerRepo.isBypassSecurityLogin()
    }

    fun clearLogin() {
        sessionManagerRepo.clearRequireLogin()
    }

    fun fetchAndUpdateDbWithRemote() {
        firebaseRemoteDataSync.fetchUserDataFromFirebase(onComplete = { profile: FirebaseProfile?, notes: List<FirebaseNote> ->
            AppLogger.createLog("OnBoardingViewModel", "fetchAndUpdateDbWithRemoteS: $profile")
            AppLogger.createLog("OnBoardingViewModel", "fetchAndUpdateDbWithRemoteS: $notes")
            profile?.let { saveBackUpProfileInPref(profile = it) }
            viewModelScope.launch {
                saveBackUpNoteInDb(firebaseNote = notes)
            }
        })
    }

    fun saveBackUpProfileInPref(profile: FirebaseProfile) {
        viewModelScope.launch {
            sessionManagerRepo.setProfilePic(profilePic = profile.profilePicUrl.orEmpty())
        }
        sessionManagerRepo.setProfileName(profilePic = profile.name.orEmpty())
        sessionManagerRepo.setProfileEmail(email = profile.email.orEmpty())
        sessionManagerRepo.setPin(pin = profile.pinHash.orEmpty())
        sessionManagerRepo.setBgTheme(themeResId = profile.theme.orEmpty())
    }

    suspend fun saveBackUpNoteInDb(firebaseNote: List<FirebaseNote>) {
        firebaseNote.map {
            it.toRoomEntity()
        }.map {
            createNoteRepository.mergeAndSave(note = it)
        }
    }

}

