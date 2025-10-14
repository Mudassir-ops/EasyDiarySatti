package com.example.easydiarysatti.ui.login

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLoginBinding
import com.example.easydiarysatti.hideKeyboard
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel by viewModels<LoginViewModel>()
    private val binding by viewBinding(FragmentLoginBinding::bind)
    private var isVerifying = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEditTextListeners()
        observeLoginState()
        clickListeners()
    }

    private fun clickListeners() {
        binding?.apply {
            btnNext.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    private fun setupEditTextListeners() {
        binding?.apply {
            val editTexts = arrayOf(edTextOne, edTextTwo, edTextThree, edTextFour)

            editTexts.forEachIndexed { index, editText ->
                editText.doAfterTextChanged { text ->
                    if (text?.length == 1 && index < editTexts.lastIndex) {
                        editTexts[index + 1].requestFocus()
                    }
                    if (index == editTexts.lastIndex && !isVerifying && text?.length == 1) {
                        val enteredPin = editTexts.joinToString("") { it.text.toString() }
                        if (enteredPin.length == 4) {
                            isVerifying = true
                            hideKeyboard()
                            viewModel.verifyPin(enteredPin)
                        }
                    }
                }
                editText.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                        if (editText.text?.isEmpty() == true && index > 0) {
                            val previous = editTexts[index - 1]
                            previous.text?.clear()
                            previous.requestFocus()
                            true
                        } else false
                    } else false
                }
            }
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { state ->
                when (state) {
                    is LoginState.Init -> {
                        enabledDisabledButton(enabled = false)
                    }

                    is LoginState.Success -> {
                        isVerifying = false
                        enabledDisabledButton(enabled = true)
                    }

                    is LoginState.Error -> {
                        isVerifying = false
                        binding?.parentView?.showSnackbar(state.message)
                        clearPinFields()
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun clearPinFields() {
        binding?.apply {
            edTextOne.text?.clear()
            edTextTwo.text?.clear()
            edTextThree.text?.clear()
            edTextFour.text?.clear()
            edTextOne.requestFocus()
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.loginFragment,
            actionId = R.id.action_loginFragment_to_mainFragment
        )
    }

    private fun enabledDisabledButton(enabled: Boolean) {
        binding?.apply {
            btnNext.alpha = if (enabled) 1F else 0.5F
            btnNext.isEnabled = enabled
        }
    }

}

