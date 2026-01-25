package com.example.easydiarysatti.ui.login

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.MainActivity
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.databinding.FragmentLoginBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel by viewModels<LoginViewModel>()
    private val binding by viewBinding(FragmentLoginBinding::bind)
    private var isVerified = false
    private var currentPin = StringBuilder()
    private val nativeViewModel by viewModels<ViewModelNative>()
    private val dotsIds = listOf(R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4)
    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBiometricAuth()
        checkBiometricAvailability()
        setupKeypadListeners()
        observeLoginState()
        clickListeners()
        setupBgTheme()
        updateDotsUi()
        setupNativeAd()
        isVerified = false
        enabledDisabledButton(enabled = false)
    }
    override fun onDestroyView() {
        // 4. Destroy ad to prevent memory leaks when leaving the screen
        nativeViewModel.destroyNative(NativeAdKey.LOGIN)
        super.onDestroyView()
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
        nativeViewModel.loadNativeAd(NativeAdKey.LOGIN)
    }
    private fun setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleNavigation()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Optional: Handle specific error codes or show snackbar
                }
            })

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Use your biometric to unlock")

        // RESOLVE CRASH: API-specific authenticator logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Supports the combined bitmask
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29: Use the legacy boolean method
            builder.setDeviceCredentialAllowed(true)
        } else {
            // API 28 and below: Combination is unsupported
            // You MUST use a negative button and CANNOT use DEVICE_CREDENTIAL
            builder.setNegativeButtonText("Use PIN")
        }

        promptInfo = builder.build()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(requireContext())

        // Define authenticators to check based on API level to avoid crash
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            // On API 28/29, check for Biometric only for the initial check
            BIOMETRIC_STRONG
        }

        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            binding?.keypadLayout?.btnBiometric?.visibility = View.VISIBLE
        } else {
            binding?.keypadLayout?.btnBiometric?.visibility = View.GONE
        }
    }

    private fun clickListeners() {
        binding?.btnNext?.setOnClickListener {
            if (isVerified) {
                handleNavigation()
            }
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { state ->
                when (state) {
                    is LoginState.Success -> {
                        isVerified = true
                        enabledDisabledButton(enabled = true)
                        currentPin.clear()
                        handleNavigation()
                    }
                    is LoginState.Error -> {
                        isVerified = false
                        binding?.parentView?.showSnackbar(state.message)
                        clearPinFields()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleNavigation() {
        val isColdStart = arguments?.getBoolean("IS_COLD_START", true) ?: true

        if (viewModel.isFirstLogin()) {
            (activity as? MainActivity)?.onLoginFinished() // Hide overlay
            viewModel.markWelcomeScreenAsSeen() //
            findNavController().safeNav(
                currentDestId = R.id.loginFragment,
                actionId = R.id.action_loginFragment_to_welcomeFragment
            )
        } else {
            if (isColdStart) {
                // Hide the curtain and move to Home
                (activity as? MainActivity)?.onLoginFinished()
                findNavController().safeNav(
                    currentDestId = R.id.loginFragment,
                    actionId = R.id.action_loginFragment_to_mainFragment
                )
            } else {
                // Background resume: just remove the curtain
                (activity as? MainActivity)?.onLoginFinished()
            }
        }
    }

    private fun onNumberClicked(number: String) {
        if (currentPin.length < 4) {
            currentPin.append(number)
            updateDotsUi()
            if (currentPin.length == 4) viewModel.verifyPin(currentPin.toString())
        }
    }

    private fun onBackspaceClicked() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updateDotsUi()
        }
    }

    private fun setupKeypadListeners() {
        binding?.keypadLayout?.apply {
            val numberButtons = listOf(btnKey0, btnKey1, btnKey2, btnKey3, btnKey4, btnKey5, btnKey6, btnKey7, btnKey8, btnKey9)
            numberButtons.forEachIndexed { index, btn ->
                btn.setOnClickListener { onNumberClicked(index.toString()) }
            }
            btnBackspace.setOnClickListener { onBackspaceClicked() }
            btnBiometric.setOnClickListener {
                try {
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    // Safe catch for any remaining edge cases
                    binding?.parentView?.showSnackbar("Biometric authentication unavailable")
                }
            }
        }
    }

    private fun clearPinFields() {
        currentPin.clear()
        updateDotsUi()
    }

    private fun enabledDisabledButton(enabled: Boolean) {
        binding?.btnNext?.apply {
            alpha = if (enabled) 1F else 0.5F
            isEnabled = enabled
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })
    }

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

    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = getThemeColor(themeResId)
        binding?.apply {
            btnNext.backgroundTintList = ColorStateList.valueOf(themeColor)
            keypadLayout.btnBiometric.imageTintList = ColorStateList.valueOf(themeColor)
            keypadLayout.btnBackspace.imageTintList = ColorStateList.valueOf(themeColor)
        }
        updateDotsUi()
    }

    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()
        applyDynamicTheme(currentTheme)
    }

    private fun updateDotsUi() {
        val themeColor = getThemeColor(sessionManagerRepo.getBgTheme())
        dotsIds.forEachIndexed { index, dotId ->
            val dotView = binding?.root?.findViewById<ImageView>(dotId)
            if (index < currentPin.length) {
                dotView?.setImageResource(R.drawable.ic_pin_dot_filled)
                dotView?.imageTintList = ColorStateList.valueOf(themeColor)
            } else {
                dotView?.setImageResource(R.drawable.ic_pin_dot_empty)
                dotView?.imageTintList = ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            }
        }
    }
}
