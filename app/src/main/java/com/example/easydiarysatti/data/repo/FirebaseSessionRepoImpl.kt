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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
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

    override suspend fun saveUserNote(createNoteEntity: CreateNoteEntity) {
        AppLogger.createLog("FirebaseRemoteDataSync", "saveUserNote: $createNoteEntity")
        val uploadedUrls = uploadNoteImages(createNoteEntity)
        val noteToSave = createNoteEntity.copy(images = uploadedUrls).toFirebaseNote()
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

    suspend fun uploadNoteImages(note: CreateNoteEntity): List<String> = coroutineScope {
        val uid = device.serial
        val storageRef = FirebaseStorage.getInstance().reference
        val imagePaths = note.images ?: emptyList()

        val uploadedUrls = imagePaths.map { localPath ->
            async(Dispatchers.IO) {
                try {
                    val file = Uri.fromFile(File(localPath))
                    val fileRef =
                        storageRef.child("users/$uid/notes/${note.noteId}/${file.lastPathSegment}")
                    fileRef.putFile(file).await()
                    fileRef.downloadUrl.await().toString()
                } catch (e: Exception) {
                    AppLogger.createLog(
                        "FirebaseRemoteDataSync",
                        "Failed to upload image $localPath: $e"
                    )
                    null
                }
            }
        }
        uploadedUrls.awaitAll().filterNotNull()
    }

}
