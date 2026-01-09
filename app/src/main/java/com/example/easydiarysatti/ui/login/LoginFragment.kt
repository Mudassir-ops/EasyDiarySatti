package com.example.easydiarysatti.ui.login

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.example.easydiarysatti.R
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
        isVerified = false
        enabledDisabledButton(enabled = false)
    }

    private fun setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleNavigation()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
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
        if (viewModel.isFirstLogin()) {
            // ONLY SHOWS ONCE: First login after setup
            viewModel.markWelcomeScreenAsSeen()
            findNavController().safeNav(
                currentDestId = R.id.loginFragment,
                actionId = R.id.action_loginFragment_to_welcomeFragment
            )
        } else {
            // SUBSEQUENT LOGINS: Skip Welcome back screen
            findNavController().safeNav(
                currentDestId = R.id.loginFragment,
                actionId = R.id.action_loginFragment_to_mainFragment
            )
        }
    }

    // --- Keypad and UI Logic ---

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

//    private fun updateDotsUi() {
//        dotsIds.forEachIndexed { index, dotId ->
//            val dotView = binding?.root?.findViewById<ImageView>(dotId)
//            dotView?.setImageResource(if (index < currentPin.length) R.drawable.ic_pin_dot_filled else R.drawable.ic_pin_dot_empty)
//        }
//    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(requireContext())
        if (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            binding?.keypadLayout?.btnBiometric?.visibility = View.VISIBLE
        }
    }

    private fun setupKeypadListeners() {
        binding?.keypadLayout?.apply {
            val numberButtons = listOf(btnKey0, btnKey1, btnKey2, btnKey3, btnKey4, btnKey5, btnKey6, btnKey7, btnKey8, btnKey9)
            numberButtons.forEachIndexed { index, btn -> btn.setOnClickListener { onNumberClicked(index.toString()) } }
            btnBackspace.setOnClickListener { onBackspaceClicked() }
            btnBiometric.setOnClickListener { biometricPrompt.authenticate(promptInfo) }
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



    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = getThemeColor(themeResId)

        binding?.apply {
            // Update Next Button
            btnNext.backgroundTintList = ColorStateList.valueOf(themeColor)

            // If you have a keypad, you might want to tint the backspace or biometric icon too
            keypadLayout.btnBiometric.imageTintList = ColorStateList.valueOf(themeColor)
            keypadLayout.btnBackspace.imageTintList = ColorStateList.valueOf(themeColor)
        }

        // Refresh dots color based on current input
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
                // Using a lighter gray for the empty state
                dotView?.imageTintList = ColorStateList.valueOf(Color.parseColor("#D1D5DB"))
            }
        }
    }

}
