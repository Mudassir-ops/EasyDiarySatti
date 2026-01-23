package com.example.easydiarysatti.ui.signup

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_ONBOARDING
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.databinding.FragmentSignUpBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {
    private val viewModel by viewModels<SignUpViewModel>()
    private val binding by viewBinding(FragmentSignUpBinding::bind)
    private var firstPin: String? = null
    private var isPinConfirmed = false
    private var currentPin = StringBuilder()
    private val nativeViewModel: ViewModelNative by viewModels()
    private val dotsIds = listOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4)
    private var canUseBiometrics = false
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val eventParams = Bundle()
        eventParams.putString("onBoardingSignup", "open_screen")
        mFirebaseAnalytics.logEvent("On_Boarding_Access_Your_Diary_Login", eventParams)
        checkBiometricAvailability()
        setupKeypadListeners()
        clickListener()
        setupInitialButtonState()
        updateStageText()
        setupNativeAd()
    }
    private fun setupNativeAd() {
        // 1. Observe the LiveData
        nativeViewModel.adViewLiveData.observe(viewLifecycleOwner) { nativeAd ->
            if (nativeAd != null) {
                val adSmallView = AdNativeSmallView(requireContext())
                binding?.flAdplaceholder?.apply {
                    removeAllViews()
                    addView(adSmallView)
                    adSmallView.setNativeAd(nativeAd)
                }
            }
        }

        // 2. Request the ad (using the ON_BOARDING or appropriate key)
        nativeViewModel.loadNativeAd(NativeAdKey.SIGNUP)
    }
    // Check if hardware is present to decide if we show the button
    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(requireContext())
        // Only checking for strong biometrics here for informational purposes
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding?.keypadLayout?.btnBiometric?.visibility = View.VISIBLE
                canUseBiometrics = true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Hide if no hardware
                binding?.keypadLayout?.btnBiometric?.visibility = View.INVISIBLE
                canUseBiometrics = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Hardware exists but user hasn't set it up in phone settings
                binding?.keypadLayout?.btnBiometric?.visibility = View.VISIBLE
                canUseBiometrics = false
            }
            else -> {
                binding?.keypadLayout?.btnBiometric?.visibility = View.INVISIBLE
                canUseBiometrics = false
            }
        }
    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply {
            putString("action_label", label)
        }
        mFirebaseAnalytics.logEvent(eventName, params)
    }
    private fun setupInitialButtonState() {
        binding?.btnNext?.apply {
            isEnabled = false
            alpha = 0.5f
            text = getString(R.string.setup_pin)
        }
    }

    private fun setupKeypadListeners() {
        binding?.keypadLayout?.apply {
            val numberButtons = listOf(
                btnKey0, btnKey1, btnKey2, btnKey3, btnKey4,
                btnKey5, btnKey6, btnKey7, btnKey8, btnKey9
            )
            numberButtons.forEachIndexed { index, button ->
                button.setOnClickListener { onNumberClicked(index.toString()) }
            }
            btnBackspace.setOnClickListener { onBackspaceClicked() }

            // Informational click listener for Sign Up phase
            btnBiometric.setOnClickListener {
                if (canUseBiometrics) {
                    binding?.parentView?.showSnackbar("Biometric authentication will be available for future logins.")
                } else {
                    binding?.parentView?.showSnackbar("Biometrics are not set up on this device.")
                }
            }
        }
    }

    // --- Rest of existing PIN logic remains unchanged ---

    private fun onNumberClicked(number: String) {
        if (currentPin.length < 4) {
            currentPin.append(number)
            updateDotsUi()

            // When we reach 4, trigger the logic immediately
            if (currentPin.length == 4) {
                handleFullPinEntry()
            }
        }
    }

    private fun onBackspaceClicked() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updateDotsUi()
            if (isPinConfirmed) {
                isPinConfirmed = false

            }
        }
    }

    private fun updateDotsUi() {
        dotsIds.forEachIndexed { index, dotId ->
            val dotView = binding?.root?.findViewById<ImageView>(dotId)
            if (index < currentPin.length) {
                dotView?.setImageResource(R.drawable.ic_pin_dot_filled)
            } else {
                dotView?.setImageResource(R.drawable.ic_pin_dot_empty)
            }
        }
    }

    private fun onPinEntry(digit: String) {
        if (currentPin.length < 4) {
            currentPin.append(digit)
            updateDotsUi()

            // Add this check to move forward automatically
            if (currentPin.length == 4) {
                handleFullPinEntry()
            }
        }
    }

    private fun handleFullPinEntry() {
        val enteredPin = currentPin.toString()

        if (firstPin == null) {
            // STEP 1: First 4 digits entered, move to confirmation
            firstPin = enteredPin
            clearPinFields() // This clears currentPin for the next entry
            updateStageText() // Should now show "Confirm your PIN"
        } else {
            // STEP 2: Confirmation digits entered, check if they match
            if (enteredPin == firstPin) {
                isPinConfirmed = true
                viewModel.savePin(enteredPin = firstPin.orEmpty())
                moveToNextScreen() // Auto-navigate to dashboard
            } else {
                // Error: PINs don't match, reset to start
                firstPin = null
                clearPinFields()
                updateStageText()
                binding?.parentView?.showSnackbar(
                    message = getString(R.string.pins_do_not_match_try_again)
                )
            }
        }
    }

    private fun updateStageText() {
        binding?.txtPinStage?.text = if (firstPin == null) {
            "Create your PIN"
        } else {
            "Confirm your PIN"
        }
    }

    private fun setButtonReadyState() {
        binding?.btnNext?.apply {
            isEnabled = true
            alpha = 1f
            text = getString(R.string.done)
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
        currentPin.clear()
        updateDotsUi()
    }

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener {
                if (isPinConfirmed) {
                    viewModel.savePin(enteredPin = firstPin.orEmpty())
                    moveToNextScreen()
                }
            }
        }
    }

    private fun moveToNextScreen() {
        val bundle = Bundle().apply {
            putBoolean(FROM_ONBOARDING, true)
        }
        findNavController().safeNav(
            currentDestId = R.id.signUpFragment,
            actionId = R.id.action_signUpFragment_to_themesFragment,
            bundle = bundle
        )
    }
}

