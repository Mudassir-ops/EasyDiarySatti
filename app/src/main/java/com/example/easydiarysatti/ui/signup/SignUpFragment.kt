package com.example.easydiarysatti.ui.signup

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_ONBOARDING
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentSignUpBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {
    private val viewModel by viewModels<SignUpViewModel>()
    private val binding by viewBinding(FragmentSignUpBinding::bind)
    private var firstPin: String? = null
    private var isPinConfirmed = false
    // NEW: Ad and Preference dependencies
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    @Inject lateinit var sharedPreferenceUtils: SharedPreferenceUtils
    private var currentPin = StringBuilder()
    private val nativeViewModel: ViewModelNative by viewModels()
    private val dotsIds = listOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4)
    private var canUseBiometrics = false
    private var isVerificationMode = false // New flag
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val eventParams = Bundle()
        eventParams.putString("onBoardingSignup", "open_screen")
        mFirebaseAnalytics.logEvent("On_Boarding_Access_Your_Diary_Login", eventParams)
        // CHECK IF PIN ALREADY EXISTS
        val savedPin = viewModel.getPin()
        if (!savedPin.isNullOrEmpty()) {
            isVerificationMode = true
            firstPin = savedPin // Treat saved pin as the reference
            binding?.txtPinStage?.text = "Enter your PIN"
        } else {
            isVerificationMode = false
            updateStageText() // Shows "Create your PIN"
        }
        setupBannerObserver()

        checkBiometricAvailability()
        setupKeypadListeners()

        clickListener()
        setupInitialButtonState()
        updateStageText()

    }
    // 1. Define this variable at the top of your Fragment class (not inside the function)
    private var isAdProcessStarted = false

    private fun setupBannerObserver() {
        if (!sharedPreferenceUtils.getAdShowStatus(BannerAdKey.PIN_SETUP.value)) {
            binding?.bannerShimmerContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            // 2. SAFETY LOCK: If we already started the timer for this screen, STOP here.
            if (isAdProcessStarted) return@observe

            // Specifically grab the ad preloaded for the PIN screen
            val preloadedAd = adMap[BannerAdKey.PIN_SETUP]

            if (preloadedAd != null) {
                // 3. ACTIVATE LOCK: Ensure this block only runs once per fragment lifecycle
                isAdProcessStarted = true

                // Show the shimmer container and start animation
                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()


                    // Check if fragment is still attached to avoid crashes
                    if (isAdded && binding != null) {

                        // 5. TURN OFF SHIMMER: Stop animation and clear the effect
                        binding?.bannerShimmerContainer?.stopShimmer()
                        binding?.bannerShimmerContainer?.setShimmer(null)

                        binding?.bannerContainer?.let { container ->
                            // Remove gray background so the shimmer has nothing to reflect off of
                            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            container.addCleanView(preloadedAd)
                            container.visibility = View.VISIBLE
                        }
                        Log.d("AdDebug", "Shimmer off, PIN_SETUP Ad visible (One-time load)")
                    }
            } else {
                // Keep container hidden while waiting for the ad to appear in the map
                binding?.bannerShimmerContainer?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
                Log.d("AdDebug", "PIN_SETUP ad not found in map yet...")
            }
        }
    }

    /* ---------- Waterfall Skip Logic ---------- */
    override fun onDestroyView() {
        super.onDestroyView()
        isAdProcessStarted = false
    }
    private fun moveToNextScreen() {
        // Look ahead: Is Theme Selection enabled?
        val showTheme = sharedPreferenceUtils.isThemeSelectionEnabled

        if (showTheme) {
            // Path 1: Move to Theme
            preLoadNextAd(BannerAdKey.THEME_SELECTION)

            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.signUpFragment, true) // Clear the PIN setup from history
                .build()

            findNavController().navigate(
                R.id.action_signUpFragment_to_themesFragment,
                Bundle().apply { putBoolean(FROM_ONBOARDING, true) },
                navOptions
            )
        } else {
            // Path 2: Theme is disabled -> SETUP FINISHED
            sharedPreferenceUtils.isFirstTimeUser = false

            findNavController().safeNav(
                currentDestId = R.id.signUpFragment,
                actionId = R.id.action_pinSetupFragment_to_mainFragment
            )
        }
    }

    private fun preLoadNextAd(adKey: BannerAdKey) {
        Log.d("AdsInformation", "PIN Setup pre-loading ad for: ${adKey.value}")
        val adView = com.google.android.gms.ads.AdView(requireContext())
        bannerViewModel.loadBannerAd(adView, adKey, requireContext())
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

        if (isVerificationMode) {
            // IF ALREADY SET: Just verify and move forward
            if (enteredPin == firstPin) {
                moveToNextScreen()
            } else {
                clearPinFields()
                binding?.parentView?.showSnackbar("Incorrect PIN, please try again")
            }
        } else {
            // IF NOT SET: Standard Create -> Confirm logic
            if (firstPin == null) {
                firstPin = enteredPin
                clearPinFields()
                updateStageText()
            } else {
                if (enteredPin == firstPin) {
                    isPinConfirmed = true
                    viewModel.savePin(enteredPin = firstPin.orEmpty())
                    moveToNextScreen()
                } else {
                    firstPin = null
                    clearPinFields()
                    updateStageText()
                    binding?.parentView?.showSnackbar(getString(R.string.pins_do_not_match_try_again))
                }
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


}

