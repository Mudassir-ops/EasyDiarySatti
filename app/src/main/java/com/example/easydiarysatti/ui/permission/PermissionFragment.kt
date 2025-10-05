package com.example.easydiarysatti.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLanguageBinding
import com.example.easydiarysatti.databinding.FragmentLanguageBinding.bind
import com.example.easydiarysatti.databinding.FragmentPermissionBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setSelectedBg
import com.example.easydiarysatti.showPermissionDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val binding by viewBinding(FragmentPermissionBinding::bind)

    private var cameraDeniedCount = 0
    private var galleryDeniedCount = 0

    private lateinit var requestCameraPermission: ActivityResultLauncher<String>
    private lateinit var requestGalleryPermission: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCameraPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    binding?.icSwitchCamera?.isChecked = true
                    cameraDeniedCount = 0
                } else {
                    binding?.icSwitchCamera?.isChecked = false
                    cameraDeniedCount++
                    if (cameraDeniedCount >= 2) {
                        showPermissionDialog(context ?: return@registerForActivityResult, this)
                    }
                }
                checkAllPermissions()
            }
        requestGalleryPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val allGranted = results.all { it.value }
                if (allGranted) {
                    binding?.icSwitchGallery?.isChecked = true
                    galleryDeniedCount = 0
                } else {
                    binding?.icSwitchGallery?.isChecked = false
                    galleryDeniedCount++
                    if (galleryDeniedCount >= 2) {
                        showPermissionDialog(context ?: return@registerForActivityResult, this)
                    }
                }
                checkAllPermissions()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
    }

    private fun checkAllPermissions() {
        val cameraGranted =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        val galleryGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            val read = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) ==
                    PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) ==
                    PackageManager.PERMISSION_GRANTED
            read && write
        }
        val allGranted = cameraGranted && galleryGranted
        binding?.btnNext?.isEnabled = allGranted
        binding?.btnNext?.alpha = if (allGranted) 1f else 0.5f
    }

    private fun clickListener() {
        binding?.apply {
            binding?.btnNext?.isEnabled = false
            binding?.btnNext?.alpha = 0.5f
            binding?.icSwitchCamera?.setOnClickListener {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
            binding?.icSwitchGallery?.setOnClickListener {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                requestGalleryPermission.launch(permissions.toTypedArray())
            }
            btnNext.setOnClickListener { moveToNextScreen() }
        }
    }

    fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.permissionFragment,
            actionId = R.id.action_permissionFragment_to_nameFragment
        )
    }
}