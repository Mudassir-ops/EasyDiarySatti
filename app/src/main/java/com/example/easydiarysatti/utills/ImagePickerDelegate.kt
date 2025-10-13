package com.example.easydiarysatti.utills

import android.Manifest
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.util.Date
import java.util.Locale

class ImagePickerDelegate(
    private val fragment: Fragment,
    private val onPickerClosed: (() -> Unit)? = null,
    private val onImagePicked: (uri: Uri?, file: File?) -> Unit,
) {
    private val context = fragment.context
    private var tempImageUri: Uri? = null
    private val cameraPermissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickFromCamera()
            } else {
                showToast("Camera permission denied")
            }
        }

    private val galleryPermissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickFromGallery()
            } else {
                showToast("Storage permission denied")
            }
        }

    private val cameraLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    onImagePicked(uri, uriToFile(uri))
                }
            } else {
                onPickerClosed?.invoke()
            }
        }

    private val galleryLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                onImagePicked(uri, uriToFile(uri))
            } else {
                onPickerClosed?.invoke()
            }
        }

    fun showPickerDialog() {
        requestGalleryPermission()
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun requestGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        galleryPermissionLauncher.launch(permission)
    }

    private fun pickFromCamera() {
        val photoFile = createImageFile()
        tempImageUri = FileProvider.getUriForFile(
            context ?: return,
            "${context.packageName}.provider",
            photoFile
        )
        cameraLauncher.launch(tempImageUri)
    }

    private fun pickFromGallery() {
        galleryLauncher.launch("image/*")
    }


    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_test_", ".jpg", storageDir)
    }

    private fun uriToFile(
        uri: Uri,
        context: Context = this.context ?: fragment.requireContext()
    ): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
        return file
    }

    private fun showToast(message: String) {
        com.example.easydiarysatti.showToast(message = message, context = context ?: return)
    }
}



