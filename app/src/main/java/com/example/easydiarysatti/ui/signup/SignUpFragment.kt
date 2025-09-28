package com.example.easydiarysatti.ui.signup

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentSignUpBinding
import com.example.easydiarysatti.hideKeyboard
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {
    private val viewModel by viewModels<SignUpViewModel>()
    private val binding by viewBinding(FragmentSignUpBinding::bind)
    private var firstPin: String? = null
    private var isPinConfirmed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEditTextListeners()
        clickListener()
        setupInitialButtonState()
    }

    private fun setupInitialButtonState() {
        binding?.btnNext?.apply {
            isEnabled = false
            alpha = 0.5f
            text = getString(R.string.setup_pin)
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
                    if (editTexts.all { it.text?.length == 1 }) {
                        val enteredPin = editTexts.joinToString("") { it.text.toString() }
                        handlePinEntry(enteredPin)
                    }
                }

                editText.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                        if (editText.text?.isEmpty() == true && index > 0) {
                            val previousEditText = editTexts[index - 1]
                            previousEditText.text?.clear()
                            previousEditText.requestFocus()
                            true
                        } else false
                    } else false
                }
            }
        }
    }

    private fun handlePinEntry(enteredPin: String) {
        if (firstPin == null) {
            firstPin = enteredPin
            clearPinFields()
        } else {
            if (firstPin == enteredPin) {
                hideKeyboard()
                setButtonReadyState()
                isPinConfirmed = true
            } else {
                hideKeyboard()
                firstPin = null
                clearPinFields()
                isPinConfirmed = false
                resetButtonState()
                binding?.parentView?.showSnackbar(
                    message = getString(R.string.pins_do_not_match_try_again)
                )
            }
        }
    }

    private fun setButtonReadyState() {
        binding?.btnNext?.apply {
            isEnabled = true
            alpha = 1f
            text = getString(R.string.done) // "Done"
        }
    }

    private fun resetButtonState() {
        binding?.btnNext?.apply {
            isEnabled = false
            alpha = 0.5f
            text = getString(R.string.setup_pin)
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

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener {
                if (isPinConfirmed) {
                    moveToNextScreen()
                }
            }
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.signUpFragment,
            actionId = R.id.action_signUpFragment_to_languageFragment
        )
    }

}

