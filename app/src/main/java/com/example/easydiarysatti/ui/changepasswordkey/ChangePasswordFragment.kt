package com.example.easydiarysatti.ui.changepasswordkey

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.databinding.FragmentChangePasswordBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.ui.signup.SignUpViewModel
import com.example.easydiarysatti.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private val viewModel by viewModels<ChangePasswordViewModel>()
    private val signUpViewModel by viewModels<SignUpViewModel>()
    private val nativeViewModel by viewModels<ViewModelNative>()

    private val binding by viewBinding(FragmentChangePasswordBinding::bind)
    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    // State Management
    private enum class ChangeStep { OLD_PIN, NEW_PIN, CONFIRM_PIN }
    private var currentStep = ChangeStep.OLD_PIN

    private var oldPinEntry: String = ""
    private var newPinEntry: String = ""
    private var currentInput = StringBuilder()

    private val dotsIds = listOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        setupBgTheme()
        setupKeypad()
        updateUiState()
//setupNativeAd()
        binding?.ivMenu?.setOnClickListener { findNavController().navigateUp() }
    }
//    private fun setupNativeAd() {
//        // 1. Observe the LiveData
//        nativeViewModel.adViewLiveData.observe(viewLifecycleOwner) { nativeAd ->
//            if (nativeAd != null) {
//                val adSmallView = AdNativeSmallView(requireContext())
//                binding?.flAdplaceholder?.apply {
//                    removeAllViews()
//                    addView(adSmallView)
//                    adSmallView.setNativeAd(nativeAd)
//                }
//            }
//        }
//
//        // 2. Request the ad (using the ON_BOARDING or appropriate key)
////        nativeViewModel.loadNativeAd(NativeAdKey.CHANGE_PASSWORD)
//    }
    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = viewModel.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }

    private fun setupKeypad() {
        binding?.keypadLayout?.apply {
            val numbers = listOf(btnKey0, btnKey1, btnKey2, btnKey3, btnKey4, btnKey5, btnKey6, btnKey7, btnKey8, btnKey9)
            numbers.forEachIndexed { index, btn ->
                btn.setOnClickListener { onNumberClicked(index.toString()) }
            }
            btnBackspace.setOnClickListener { onBackspaceClicked() }
            // Hide biometric button for password changing logic
            btnBiometric.visibility = View.INVISIBLE
        }
    }

    private fun onNumberClicked(number: String) {
        if (currentInput.length < 4) {
            currentInput.append(number)
            updateDotsUi()

            if (currentInput.length == 4) {
                processCompletedInput(currentInput.toString())
            }
        }
    }

    private fun onBackspaceClicked() {
        if (currentInput.isNotEmpty()) {
            currentInput.deleteCharAt(currentInput.length - 1)
            updateDotsUi()
        }
    }

    private fun processCompletedInput(pin: String) {
        when (currentStep) {
            ChangeStep.OLD_PIN -> {
                // Get PIN from signUpViewModel (or wherever you store the master PIN)
                if (pin == signUpViewModel.getPin()) {
                    oldPinEntry = pin
                    currentStep = ChangeStep.NEW_PIN
                    resetInput()
                } else {
                    showPopupDialog(getString(R.string.old_pin_is_incorrect))
                    resetInput()
                }
            }
            ChangeStep.NEW_PIN -> {
                newPinEntry = pin
                currentStep = ChangeStep.CONFIRM_PIN
                resetInput()
            }
            ChangeStep.CONFIRM_PIN -> {
                if (pin == newPinEntry) {
                    signUpViewModel.savePin(pin)
                    showSuccessDialog(getString(R.string.pin_changed_successfully))
                } else {
                    showPopupDialog(getString(R.string.new_pins_do_not_match))
                    currentStep = ChangeStep.NEW_PIN
                    resetInput()
                }
            }
        }
        updateUiState()
    }

    private fun updateUiState() {
        binding?.txtPinStage?.text = when (currentStep) {
            ChangeStep.OLD_PIN -> getString(R.string.enter_old_passkey)
            ChangeStep.NEW_PIN -> getString(R.string.enter_new_passkey)
            ChangeStep.CONFIRM_PIN -> getString(R.string.confirm_new_passkey)
        }
    }
    private fun showPopupDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.success))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                findNavController().navigateUp()
            }
            .show()
    }
    // Helper to get color once for various UI elements
    private fun getThemeColor(themeResId: Int?): Int {
        return when (themeResId) {
            R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
            R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
            R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
            R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
            R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
            else -> ContextCompat.getColor(requireContext(), R.color.app_primary_color)
        }
    }

    private fun updateDotsUi() {
        val themeColor = getThemeColor(sessionManagerRepo.getBgTheme())

        dotsIds.forEachIndexed { index, dotId ->
            val dotView = binding?.root?.findViewById<ImageView>(dotId)
            if (index < currentInput.length) {
                dotView?.setImageResource(R.drawable.ic_pin_dot_filled)
                dotView?.imageTintList = ColorStateList.valueOf(themeColor)
            } else {
                dotView?.setImageResource(R.drawable.ic_pin_dot_empty)
                // Using a lighter gray for the empty state
                dotView?.imageTintList = ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            }
        }
    }
    private fun resetInput() {
        currentInput.clear()
        updateDotsUi()
    }

    private fun showError(message: String) {
        binding?.parentView?.showSnackbar(message)
    }
}