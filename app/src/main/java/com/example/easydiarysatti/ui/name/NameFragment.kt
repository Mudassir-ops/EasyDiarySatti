package com.example.easydiarysatti.ui.name

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLanguageBinding
import com.example.easydiarysatti.databinding.FragmentLanguageBinding.bind
import com.example.easydiarysatti.databinding.FragmentNameBinding
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.ui.language.LanguageViewModel
import com.example.easydiarysatti.utills.loadImage
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NameFragment : Fragment(R.layout.fragment_name) {
    private val viewModel by viewModels<NameViewModel>()
    private val binding by viewBinding(FragmentNameBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            imgIntroOne.loadImage(resourceId = R.drawable.name_pic)
        }
    }
}