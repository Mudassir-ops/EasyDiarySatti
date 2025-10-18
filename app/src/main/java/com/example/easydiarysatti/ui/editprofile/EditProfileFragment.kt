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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = ImagePickerDelegate(this) { uri, file ->
            showImageCropDialog(imagePath = file?.path ?: return@ImagePickerDelegate, btnDone = {
                profilePic = it.toString()
                binding?.ivProfile?.setImage(drawable = it)
            }, closeDialog = {

            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            etPname.doOnTextChanged { text, _, _, _ ->

            }
            etPmail.doOnTextChanged { text, _, _, _ ->

            }

            btnNext.setOnClickListener {
                sessionManagerRepo.setProfilePic(profilePic = profilePic)
                viewModel.saveName(name = etPname.text.toString())
                viewModel.saveEmail(mail = etPmail.text.toString())
                findNavController().navigateUp()
            }
        }
    }


    private fun setupDefaultValues() {
        binding?.apply {
            val profilePic = sessionManagerRepo.getprofilePic().orEmpty()
            if (!profilePic.isEmpty()) {
                this@EditProfileFragment.profilePic = profilePic
                ivProfile.setImage(drawable = profilePic.toUri())
            }
            val savedName = viewModel.getName()
            if (savedName?.isNotEmpty() == true) {
                etPname.setText(savedName)
            }
            val savedEmail = viewModel.getEmail()
            if (savedEmail?.isNotEmpty() == true) {
                etPmail.setText(savedEmail)
            }
        }
    }
}