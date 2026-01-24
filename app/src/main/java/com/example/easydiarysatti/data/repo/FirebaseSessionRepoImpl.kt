package com.example.easydiarysatti.data.repo

import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.mapper.toFirebaseNote
import com.example.easydiarysatti.domain.model.Device
import com.example.easydiarysatti.domain.repo.FirebaseRemoteDataSync
import com.google.firebase.database.DatabaseReference

class FirebaseSessionRepoImpl(
    private val database: DatabaseReference,
    private val device: Device
) : FirebaseRemoteDataSync {

    private fun userRef(): DatabaseReference {
        val uid = device.serial
        return database.child("users").child(uid).child("profile")
    }

    override fun saveProfileName(name: String) {
        userRef().child("name").setValue(name)
    }

    override fun saveProfilePic(pic: String) {
        userRef().child("profilePicUrl").setValue(pic)
    }

    override fun saveEmail(email: String) {
        userRef().child("email").setValue(email)
    }

    override fun savePinHash(pinHash: String) {
        userRef().child("pinHash").setValue(pinHash)
    }

    override fun saveTheme(themeId: Int) {
        userRef().child("theme").setValue(themeId)
    }

    override fun saveUserNote(createNoteEntity: CreateNoteEntity) {
        AppLogger.createLog("FirebaseRemoteDataSync", "saveUserNote: $createNoteEntity")
        userRef().child("notes").push()
            .setValue(createNoteEntity.toFirebaseNote())
            .addOnSuccessListener {
                AppLogger.createLog("FirebaseRemoteDataSync", "Note saved successfully")
            }
            .addOnFailureListener { e ->
                AppLogger.createLog("FirebaseRemoteDataSync", "Failed to save note: $e")
            }


    }

}
