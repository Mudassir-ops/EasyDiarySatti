package com.example.easydiarysatti.ui.name

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentNameBinding
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

    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val binding by viewBinding(FragmentNameBinding::bind)
    @Inject lateinit var internetManager: InternetManager
    @Inject lateinit var sharedPref: SharedPreferenceUtils

    // Named Handler + Runnable so the pending postDelayed can be cancelled in
    // onDestroyView. Without this, the 100ms callback fires on a destroyed view.
    private val keyboardHandler = Handler(Looper.getMainLooper())
    private var keyboardRunnable: Runnable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        setupBannerObserver()

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

    private fun setupBannerObserver() {
        if (sharedPref.isAppPurchased||!internetManager.isInternetConnected || !sharedPref.getAdShowStatus(BannerAdKey.START_WRITING.value)) {
            binding?.shimmerBannerContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            val preloadedAd = adMap[BannerAdKey.START_WRITING]

            if (preloadedAd != null) {
                binding?.shimmerBannerContainer?.visibility = View.VISIBLE
                binding?.shimmerBannerContainer?.startShimmer()

                if (isAdded && binding != null) {
                    binding?.shimmerBannerContainer?.stopShimmer()
                    binding?.shimmerBannerContainer?.setShimmer(null)

                    binding?.bannerContainer?.let { container ->
                        container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        container.addCleanView(preloadedAd)
                        container.visibility = View.VISIBLE
                    }
                    Log.d("AdDebug", "Shimmer off, Preloaded Ad visible (One-time load)")
                }
            } else {
                binding?.shimmerBannerContainer?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
                Log.d("AdDebug", "START_WRITING ad not found in map yet...")
            }
        }
    }

    /* ---------- Waterfall Navigation driven by onboarding flow JSON ----------
     *
     * We ask the JSON "what comes after name?" and navigate there directly.
     * No boolean flags are read here — the JSON already encodes the full
     * active case so the waterfall is automatically correct for any case:
     *
     *   case_full           → name → pin   → theme → home
     *   case_no_pin         → name → theme → home
     *   case_no_theme       → name → pin   → home
     *   case_no_pin_no_theme → name → home
     */
    fun moveToNextScreen() {
        val nextScreen = sharedPref.nextScreenAfter(SharedPreferenceUtils.SCREEN_NAME)
        Log.d("NameFragment", "Next screen after name: $nextScreen")

        when (nextScreen) {
            SharedPreferenceUtils.SCREEN_PIN -> {
                preLoadNextAd(BannerAdKey.PIN_SETUP)
                findNavController().safeNav(
                    currentDestId = R.id.nameFragment,
                    actionId = R.id.action_nameFragment_to_signUpFragment
                )
            }
            SharedPreferenceUtils.SCREEN_THEME -> {
                preLoadNextAd(BannerAdKey.THEME_SELECTION)
                findNavController().safeNav(
                    currentDestId = R.id.nameFragment,
                    actionId = R.id.action_nameFragment_to_themeFragment
                )
            }
            SharedPreferenceUtils.SCREEN_HOME, null -> {
                sharedPref.isFirstTimeUser = false
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
                keyboardRunnable?.let { keyboardHandler.removeCallbacks(it) }
                keyboardRunnable = Runnable {
                    if (!isAdded || view == null) return@Runnable
                    val scrollPos = binding?.edTextName?.bottom ?: 0
                    binding?.nestedScrollView?.smoothScrollTo(0, scrollPos)
                }.also { keyboardHandler.postDelayed(it, 100) }
            } else {
                keyboardRunnable?.let { keyboardHandler.removeCallbacks(it) }
                keyboardRunnable = null
                if (isAdded && view != null) {
                    binding?.nestedScrollView?.smoothScrollTo(0, 0)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        keyboardRunnable?.let { keyboardHandler.removeCallbacks(it) }
        keyboardRunnable = null
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
                if (isValid) {
                    btnNext.visibility = View.VISIBLE
                } else {
                    btnNext.visibility = View.GONE
                }
            }
        }
    }
}