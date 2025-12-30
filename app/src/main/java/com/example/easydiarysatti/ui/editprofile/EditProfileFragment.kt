package com.example.easydiarysatti.ui.editprofile

import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentEditProfileBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.utills.ImagePickerDelegate
import com.example.easydiarysatti.utills.setImage
import com.example.easydiarysatti.utills.showImageCropDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val binding by viewBinding(FragmentEditProfileBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate
    private var profilePic = ""
    private val viewModel by viewModels<NameViewModel>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializing the ImagePicker with safety checks in the callback
        imagePicker = ImagePickerDelegate(this) { uri, file ->
            val path = file?.path ?: return@ImagePickerDelegate

            showImageCropDialog(
                imagePath = path,
                btnDone = { drawable ->
                    // FIX: Ensure fragment is attached and view exists before accessing binding
                    if (isAdded && getView() != null) {
                        profilePic = drawable.toString()
                        binding?.ivProfile?.setImage(drawable = drawable)
                    }
                },
                closeDialog = {
                    // Safety check if you add logic here later
                    if (isAdded && getView() != null) {
                        // Handle dialog close if needed
                    }
                }
            )
        }

        setupClickListeners()
        setupDefaultValues()
    }

    private fun setupClickListeners() {
        binding?.apply {
            ivEditProfile.setOnClickListener {
                imagePicker.pickFromGalleryWithPermission()
            }

            ivProfile.setOnClickListener {
                imagePicker.pickFromGalleryWithPermission()
            }

            etPname.doOnTextChanged { _, _, _, _ -> }
            etPmail.doOnTextChanged { _, _, _, _ -> }

            btnNext.setOnClickListener {
                sessionManagerRepo.setProfilePic(profilePic)
                viewModel.saveName(etPname.text.toString())
                viewModel.saveEmail(etPmail.text.toString())
                findNavController().navigateUp()
            }
        }
    }

    private fun setupDefaultValues() {
        val currentBinding = binding ?: return

        sessionManagerRepo.getprofilePic()?.takeIf { it.isNotEmpty() }?.let {
            profilePic = it
            currentBinding.ivProfile.setImage(drawable = it.toUri())
        }

        viewModel.getName()?.takeIf { it.isNotEmpty() }?.let {
            currentBinding.etPname.setText(it)
        }

        viewModel.getEmail()?.takeIf { it.isNotEmpty() }?.let {
            currentBinding.etPmail.setText(it)
        }
    }
}