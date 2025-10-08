package com.example.easydiarysatti.ui.editprofile

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentEditProfileBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
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

    @Inject
    private lateinit var sessionManagerRepo: SessionManagerRepo

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
    }

    private fun setupClickListeners() {
        binding?.apply {
            ivEditProfile.setOnClickListener {
                imagePicker.showPickerDialog()
            }
            ivProfile.setOnClickListener {
                imagePicker.showPickerDialog()
            }
            etPname.doOnTextChanged { text, _, _, _ ->

            }
            etPmail.doOnTextChanged { text, _, _, _ ->

            }

            btnNext.setOnClickListener {
                sessionManagerRepo.setProfilePic(profilePic = profilePic)
            }

        }
    }

}