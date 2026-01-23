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
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.databinding.FragmentNameBinding
import com.example.easydiarysatti.enableResize
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setKeyboardVisibilityListener
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NameFragment : Fragment(R.layout.fragment_name) {
    private val viewModel by viewModels<NameViewModel>()
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val binding by viewBinding(FragmentNameBinding::bind)
    private val nativeViewModel: ViewModelNative by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val eventParams = Bundle()
        eventParams.putString("OnboardingName", "open_screen")
        mFirebaseAnalytics.logEvent("On_Boarding_Enter_Your_Name", eventParams)
        binding?.apply {
            adjustScreenKeyboard()
            clickListener()
            setupNativeAd()
            imgIntroOne.loadImage(resourceId = R.drawable.bg_home_ic)
            val savedName = viewModel.getName()
            if (savedName?.isNotEmpty() == true) {
                edTextName.setText(savedName)
            }
        }
    }
    private fun setupNativeAd() {
        // 1. Observe the LiveData
        nativeViewModel.adViewLiveData.observe(viewLifecycleOwner) { nativeAd ->
            if (nativeAd != null) {
                val adLargeView = AdNativeLargeView(requireContext())
                binding?.flAdplaceholder?.apply {
                    removeAllViews()
                    addView(adLargeView)
                    adLargeView.setNativeAd(nativeAd)
                }
            }
        }

        // 2. Request the ad (using the ON_BOARDING or appropriate key)
        nativeViewModel.loadNativeAd(NativeAdKey.PERMISSION)
    }
    private fun adjustScreenKeyboard() {
        setKeyboardVisibilityListener { isVisible ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (isVisible) {
                    // 1. Keyboard is OPEN: Hide the ad and resize screen
                    enableResize(true)
                    binding?.flAdplaceholder?.visibility = View.GONE

                    binding?.nestedScrollView?.post {
                        if (view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            binding?.nestedScrollView?.smoothScrollTo(0, binding?.edTextName?.top ?: 0)
                        }
                    }
                } else {
                    // 2. Keyboard is CLOSED: Show the ad again
                    enableResize(false)

                    // Only show if we actually have an ad loaded
                    if (nativeViewModel.adViewLiveData.value != null) {
                        binding?.flAdplaceholder?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener {
                val eventParams = Bundle()
                eventParams.putString("OnboardingName", "next_click")
                mFirebaseAnalytics.logEvent("On_Boarding_Enter_Your_Name_Next", eventParams)
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