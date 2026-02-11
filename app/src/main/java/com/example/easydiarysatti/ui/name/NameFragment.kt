package com.example.easydiarysatti.ui.name

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentNameBinding
import com.example.easydiarysatti.enableResize
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setKeyboardVisibilityListener
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class NameFragment : Fragment(R.layout.fragment_name) {
    private val viewModel by viewModels<NameViewModel>()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()

    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val binding by viewBinding(FragmentNameBinding::bind)

    @Inject lateinit var sharedPref: SharedPreferenceUtils // Inject Preferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        setupBannerObserver() // 1. Setup Banner Display

//        // Load the ad for THIS screen (Name Setup)
//        val currentAdView = com.google.android.gms.ads.AdView(requireContext())
//        bannerViewModel.loadBannerAd(currentAdView, BannerAdKey.START_WRITING, requireContext())

        val eventParams = Bundle()
        eventParams.putString("OnboardingName", "open_screen")
        mFirebaseAnalytics.logEvent("On_Boarding_Enter_Your_Name", eventParams)

        binding?.apply {
            handleKeyboard()
            clickListener()

            imgIntroOne.loadImage(resourceId = R.drawable.bg_home_ic)
            val savedName = viewModel.getName()
            if (savedName?.isNotEmpty() == true) {
                edTextName.setText(savedName)
            }
        }
    }

    /* ---------- Banner Ad Implementation ---------- */

    // 1. Define this variable at the top of your Fragment class (not inside the function)
//    private var isAdProcessStarted = false

    private fun setupBannerObserver() {
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            // 2. SAFETY LOCK: If we already started the shimmer/load process, ignore all future updates
//            if (isAdProcessStarted) return@observe

            // Specifically grab the ad preloaded for this screen
            val preloadedAd = adMap[BannerAdKey.START_WRITING]

            if (preloadedAd != null) {
                // 3. ACTIVATE LOCK: Set this to true immediately
//                isAdProcessStarted = true

                // Show and start the shimmer
                binding?.shimmerBannerContainer?.visibility = View.VISIBLE
                binding?.shimmerBannerContainer?.startShimmer()


                    // Check if fragment is still alive to avoid crashes
                    if (isAdded && binding != null) {

                        // 5. TURN OFF SHIMMER: Stop animation and clear it
                        binding?.shimmerBannerContainer?.stopShimmer()
                        binding?.shimmerBannerContainer?.setShimmer(null)

                        binding?.bannerContainer?.let { container ->
                            // Remove gray placeholder color and add the ad
                            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            container.addCleanView(preloadedAd)
                            container.visibility = View.VISIBLE
                        }
                        Log.d("AdDebug", "Shimmer off, Preloaded Ad visible (One-time load)")
                    }

            } else {
                // If the ad isn't ready, keep the container hidden for now
                binding?.shimmerBannerContainer?.visibility = View.GONE
                Log.d("AdDebug", "START_WRITING ad not found in map yet...")
            }
        }
    }
    /* ---------- Waterfall Skip Logic ---------- */

    fun moveToNextScreen() {
        // Read the look-ahead flags
        val showPin = sharedPref.isPinSetupEnabled
        val showTheme = sharedPref.isThemeSelectionEnabled

        when {
            // Path 1: Pin Setup is enabled
            showPin -> {
                preLoadNextAd(BannerAdKey.PIN_SETUP)
                findNavController().safeNav(
                    currentDestId = R.id.nameFragment,
                    actionId = R.id.action_nameFragment_to_signUpFragment // Update ID as needed
                )
            }

            // Path 2: Pin Skip -> Check Theme Selection
            showTheme -> {
                preLoadNextAd(BannerAdKey.THEME_SELECTION)
                findNavController().safeNav(
                    currentDestId = R.id.nameFragment,
                    actionId = R.id.action_nameFragment_to_themeFragment
                )
            }

            // Path 3: All Skips -> Go Home
            else -> {
                sharedPref.isFirstTimeUser = false // Mark setup complete
                findNavController().safeNav(
                    currentDestId = R.id.nameFragment,
                    actionId = R.id.action_nameFragment_to_mainFragment
                )
            }
        }
    }

    private fun preLoadNextAd(adKey: BannerAdKey) {
        Log.d("AdsInformation", "NameFragment pre-loading ad for: ${adKey.value}")
        val adView = com.google.android.gms.ads.AdView(requireContext())
        bannerViewModel.loadBannerAd(adView, adKey, requireContext())
    }

    /* ---------- UI Logic ---------- */
    private fun handleKeyboard() {
        setKeyboardVisibilityListener { isVisible ->
            if (isVisible) {
                binding?.nestedScrollView?.postDelayed({
                    // Scroll to the bottom of the EditText, NOT the whole root height
                    // This prevents the ScrollView from trying to scroll "past" the banner
                    val scrollPos = binding?.edTextName?.bottom ?: 0
                    binding?.nestedScrollView?.smoothScrollTo(0, scrollPos)
                }, 100)
            } else {
                binding?.nestedScrollView?.smoothScrollTo(0, 0)
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
                btnNext.isEnabled = isValid
                btnNext.alpha = if (isValid) 1f else 0.6f
            }
        }
    }
}