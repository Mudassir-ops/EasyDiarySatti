package com.example.easydiarysatti.ui.changepasswordkey

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentChangePasswordBinding
import com.example.easydiarysatti.databinding.PwdItemLayoutBinding
import com.example.easydiarysatti.hideKeyboard
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.ui.signup.SignUpViewModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {
    private val viewModel by viewModels<ChangePasswordViewModel>()
    private val signUpViewModel by viewModels<SignUpViewModel>()
    private val binding by viewBinding(FragmentChangePasswordBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScreenUi()
        setupBgTheme()
        setupPinAutoMove()
    }

    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = viewModel.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }

    private fun setupScreenUi() {
        binding?.apply {
            oldPwdKeyLayout.txtSetPasswordDescription.text = getString(R.string.enter_old_passkey)
            newPwdKeyLayout.txtSetPasswordDescription.text = getString(R.string.enter_new_passkey)
            confirmPwdKeyLayout.txtSetPasswordDescription.text =
                getString(R.string.confirm_new_passkey)

            btnNext.setOnClickListener {
                val oldPwd = getPinFromLayout(oldPwdKeyLayout)
                val newPwd = getPinFromLayout(newPwdKeyLayout)
                val confirmPwd = getPinFromLayout(confirmPwdKeyLayout)
                if (validatePins(oldPwd, newPwd, confirmPwd)) {
                    saveNewPin(newPwd)
                } else {
                    resetPinLayouts(oldPwdKeyLayout)
                    resetPinLayouts(newPwdKeyLayout)
                    resetPinLayouts(confirmPwdKeyLayout)
                }
            }
        }
    }


    private fun getPinFromLayout(binding: PwdItemLayoutBinding): String {
        return binding.edTextOne.text.toString() +
                binding.edTextTwo.text.toString() +
                binding.edTextThree.text.toString() +
                binding.edTextFour.text.toString()
    }

    private fun validatePins(oldPin: String, newPin: String, confirmPin: String): Boolean {
        val storedPin = signUpViewModel.getPin()
        return when {
            oldPin.length < 4 || newPin.length < 4 || confirmPin.length < 4 -> {
                showToast(getString(R.string.please_enter_all_4_digits_in_each_field))
                false
            }

            oldPin != storedPin -> {
                showToast(getString(R.string.old_pin_is_incorrect))
                false
            }

            newPin != confirmPin -> {
                showToast(getString(R.string.new_pins_do_not_match))
                false
            }

            else -> true
        }
    }

    private fun saveNewPin(newPin: String) {
        signUpViewModel.savePin(enteredPin = newPin)
        showToast(getString(R.string.pin_changed_successfully))

    }

    private fun showToast(message: String) {
        binding?.parentView?.showSnackbar(message = message)
    }

    private fun setupPinAutoMove() {
        val layouts = listOf(
            binding?.oldPwdKeyLayout,
            binding?.newPwdKeyLayout,
            binding?.confirmPwdKeyLayout
        )

        layouts.forEachIndexed { layoutIndex, pinLayout ->
            pinLayout?.apply {
                val editTexts = listOf(edTextOne, edTextTwo, edTextThree, edTextFour)

                editTexts.forEachIndexed { index, editText ->
                    editText.doAfterTextChanged { text ->
                        if (text?.length == 1) {
                            if (index < editTexts.lastIndex) {
                                editTexts[index + 1].requestFocus()
                            } else {
                                if (layoutIndex < layouts.lastIndex) {
                                    layouts[layoutIndex + 1]?.edTextOne?.requestFocus()
                                } else {
                                    editText.clearFocus()
                                    hideKeyboard()
                                }
                            }
                        }
                    }

                    editText.setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                            if (editText.text?.isEmpty() == true) {
                                if (index > 0) {
                                    editTexts[index - 1].text?.clear()
                                    editTexts[index - 1].requestFocus()
                                } else if (layoutIndex > 0) {
                                    val prevLayout = layouts[layoutIndex - 1]
                                    prevLayout?.edTextFour?.requestFocus()
                                    prevLayout?.edTextFour?.text?.clear()
                                }
                                true
                            } else false
                        } else false
                    }
                }
            }
        }
    }


    private fun resetPinLayouts(pinBinding: PwdItemLayoutBinding) {
        pinBinding.apply {
            edTextOne.text?.clear()
            edTextTwo.text?.clear()
            edTextThree.text?.clear()
            edTextFour.text?.clear()
            edTextOne.requestFocus()
        }
    }

}