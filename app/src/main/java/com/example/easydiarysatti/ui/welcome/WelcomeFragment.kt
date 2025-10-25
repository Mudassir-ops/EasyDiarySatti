package com.example.easydiarysatti.ui.welcome

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentWelcomeBinding
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    private val viewModel by viewModels<WelcomeViewModel>()
    private val binding by viewBinding(FragmentWelcomeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            txtWelcomeBack.text = getString(R.string.welcome_back, viewModel.getName())
            imgIntroOne.loadImage(resourceId = R.drawable.name_pic)
            clickListeners()
        }
    }

    private fun clickListeners() {
        binding?.apply {
            btnNext.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.welcomeFragment,
            actionId = R.id.action_welcomeFragment_to_mainFragment
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }
}