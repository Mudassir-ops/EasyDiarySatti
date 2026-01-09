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

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var imagePicker: ImagePickerDelegate
    private var profilePic = ""
    private val viewModel by viewModels<NameViewModel>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEditProfileBinding.bind(view)
        // Initializing the ImagePicker with safety checks
        imagePicker = ImagePickerDelegate(this) { uri, file ->
            val path = file?.path ?: return@ImagePickerDelegate

            showImageCropDialog(
                imagePath = path,
                btnDone = { drawable ->
                    _binding?.let {
                        profilePic = drawable.toString()
                        it.ivProfile.setImage(drawable)
                    }
                }
                ,
                closeDialog = {
                    // Safety check for dialog closure
                    if (view != null) {

                        // Handle logic if needed
                    }
                }
            )
        }

        setupClickListeners()
        setupDefaultValues()
    }

    private fun setupClickListeners() {
        // Using binding?.apply is safe here as it's called directly in onViewCreated
        binding?.apply {
            ivEditProfile.setOnClickListener {
                imagePicker.pickFromGalleryWithPermission()
            }

            ivProfile.setOnClickListener {
                imagePicker.pickFromGalleryWithPermission()
            }

            // Note: If these are empty, ensure you aren't missing logic inside
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
        // Create a local reference to binding to ensure null-safety throughout the function
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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}