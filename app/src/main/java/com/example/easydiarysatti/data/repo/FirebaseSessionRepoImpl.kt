package com.example.easydiarysatti.data.repo

import android.net.Uri
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.mapper.toFirebaseNote
import com.example.easydiarysatti.domain.model.Device
import com.example.easydiarysatti.domain.model.FirebaseNote
import com.example.easydiarysatti.domain.model.FirebaseProfile
import com.example.easydiarysatti.domain.repo.FirebaseRemoteDataSync
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

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

    override suspend fun saveProfilePic(pic: String) {
        val uploadedUrl = uploadProfilePic(picPath = pic)
        uploadedUrl?.let {
            userRef().child("profilePicUrl").setValue(it)
        }
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

    override suspend fun saveUserNote(createNoteEntity: CreateNoteEntity) {
        AppLogger.createLog("FirebaseRemoteDataSync", "saveUserNote: $createNoteEntity")
        val noteToSave = createNoteEntity.copy().toFirebaseNote()
        val noteKey = noteToSave.noteId.toString()
        userRef().child("notes").child(noteKey)
            .setValue(noteToSave)
            .addOnSuccessListener {
                AppLogger.createLog("FirebaseRemoteDataSync", "Note saved successfully")
            }
            .addOnFailureListener { e ->
                AppLogger.createLog("FirebaseRemoteDataSync", "Failed to save note: $e")
            }
    }


    override fun fetchUserDataFromFirebase(
        onComplete: (profile: FirebaseProfile?, notes: List<FirebaseNote>) -> Unit
    ) {
        val uid = device.serial
        val userRef = database.child("users").child(uid)
        userRef.child("profile").get().addOnSuccessListener { profileSnapshot ->
            val profile = profileSnapshot.getValue(FirebaseProfile::class.java)
            val notes = profileSnapshot.child("notes").children
                .mapNotNull { it.getValue(FirebaseNote::class.java) }
            onComplete(profile, notes)
        }.addOnFailureListener {
            onComplete(null, emptyList())
        }
    }

    suspend fun uploadProfilePic(picPath: String): String? = withContext(Dispatchers.IO) {
        try {
            val uid = device.serial
            val storageRef = FirebaseStorage.getInstance().reference
            val file = Uri.fromFile(File(picPath))
            val fileRef = storageRef.child("users/$uid/profile/${file.lastPathSegment}")
            fileRef.putFile(file).await()
            fileRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            AppLogger.createLog(
                "FirebaseRemoteDataSync",
                "Failed to upload profile pic $picPath: $e"
            )
            null
        }
    }


}
