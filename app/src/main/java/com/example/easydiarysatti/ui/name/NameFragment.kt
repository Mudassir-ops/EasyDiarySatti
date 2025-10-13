package com.example.easydiarysatti.ui.name

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentNameBinding
import com.example.easydiarysatti.enableResize
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setKeyboardVisibilityListener
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NameFragment : Fragment(R.layout.fragment_name) {
    private val viewModel by viewModels<NameViewModel>()
    private val binding by viewBinding(FragmentNameBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            adjustScreenKeyboard()
            clickListener()
            imgIntroOne.loadImage(resourceId = R.drawable.name_pic)
            val savedName = viewModel.getName()
            if (savedName?.isNotEmpty() == true) {
                edTextName.setText(savedName)
            }
        }
    }
    private fun adjustScreenKeyboard() {
        setKeyboardVisibilityListener { isVisible ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (isVisible) {
                    enableResize(true)
                    binding?.nestedScrollView?.post {
                        if (view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED
                            )
                        ) {
                            binding?.nestedScrollView?.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                } else {
                    enableResize(false)
                }
            }
        }
    }

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener {
                if (edTextName.text?.isEmpty() == true) {
                    binding?.nestedScrollView?.showSnackbar(
                        message = getString(R.string.enterName)
                    )
                    edTextName.error = getString(R.string.enterName)
                } else {
                    viewModel.saveName(name = edTextName.text.toString())
                    moveToNextScreen()
                }
            }

            edTextName.doOnTextChanged { text, _, _, _ ->
                val isValid = !text.isNullOrEmpty()
                binding.apply {
                    btnNext.isEnabled = isValid
                    btnNext.alpha = if (isValid) 1f else 0.6f
                }
            }

        }
    }

    fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.nameFragment, actionId = R.id.action_nameFragment_to_signUpFragment
        )
    }

}