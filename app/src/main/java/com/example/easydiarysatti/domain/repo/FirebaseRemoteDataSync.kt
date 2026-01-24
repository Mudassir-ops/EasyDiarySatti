package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.model.FirebaseNote
import com.example.easydiarysatti.domain.model.FirebaseProfile

interface FirebaseRemoteDataSync {

    fun saveProfileName(name: String)
    fun saveProfilePic(pic: String)
    fun saveEmail(email: String)
    fun savePinHash(pinHash: String)
    fun saveTheme(themeId: Int)

    fun saveUserNote(createNoteEntity: CreateNoteEntity)

    fun fetchUserDataFromFirebase(
        onComplete: (
            profile: FirebaseProfile?,
            notes: List<FirebaseNote>
        ) -> Unit
    )
}