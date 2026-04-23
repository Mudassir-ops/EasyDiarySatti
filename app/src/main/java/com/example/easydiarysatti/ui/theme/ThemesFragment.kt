package com.example.easydiarysatti.ui.theme

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_ONBOARDING
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentThemesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue
import kotlin.math.abs

@AndroidEntryPoint
class ThemesFragment : Fragment(R.layout.fragment_themes) {

    private val viewModel by viewModels<ThemesViewModel>()
    private val viewModelEntrance by viewModels<ViewModelEntrance>()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()

    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val binding by viewBinding(FragmentThemesBinding::bind)
    private var themeAdapter: ThemeAdapter? = null

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var sessionManagerRepo: SessionManagerRepo

    private val themesList: List<Int> by lazy {
        listOf(
            R.drawable.theme_2,
            R.drawable.theme_1,
            R.drawable.theme_3,
            R.drawable.theme_4,
            R.drawable.theme_5,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        logAnalyticsEvent("On_Boarding_Choose_Your_Theme", "open_screen")
        themeAdapter = ThemeAdapter(themes = themesList, onThemeClick = {})
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBannerObserver()
        clickListener()
        setupBgTheme()

        binding?.apply {
            themeViewPager.adapter = themeAdapter
            themeViewPager.offscreenPageLimit = 4
            themeViewPager.setPageTransformer { page, position ->
                page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
            }
            themeViewPager.setCurrentItem(1, false)
        }
    }

    private var isAdProcessStarted = false

    private fun setupBannerObserver() {
        if (!sharedPref.getAdShowStatus(BannerAdKey.THEME_SELECTION.value)) {
            binding?.bannerShimmerContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            if (isAdProcessStarted) return@observe
            val preloadedAd = adMap[BannerAdKey.THEME_SELECTION]
            if (preloadedAd != null) {
                isAdProcessStarted = true
                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()
                if (isAdded && binding != null) {
                    binding?.bannerShimmerContainer?.stopShimmer()
                    binding?.bannerShimmerContainer?.setShimmer(null)
                    binding?.bannerContainer?.let { container ->
                        container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        container.addCleanView(preloadedAd)
                        container.visibility = View.VISIBLE
                    }
                    Log.d("AdDebug", "Shimmer off, THEME_SELECTION Ad visible")
                }
            } else {
                binding?.bannerShimmerContainer?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isAdProcessStarted = false
    }

    private fun clickListener() {
        binding?.apply {
            btnSelect.setOnClickListener {
                logAnalyticsEvent("On_Boarding_Choose_Your_Theme_Select", "select_click")
                val currentPosition = themeViewPager.currentItem
                val selectedThemeResId = themesList[currentPosition]
                sessionManagerRepo.setBgTheme(themeResId = selectedThemeResId)
                moveToNextScreen()
            }
            btnLater.setOnClickListener {
                logAnalyticsEvent("On_Boarding_Choose_Your_Theme_Later", "later_click")
                moveToNextScreen()
            }
            btnBack.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    fun moveToNextScreen() {
        if (arguments?.getBoolean(FROM_ONBOARDING) == true) {
            sessionManagerRepo.setOnBoardingDoneOnce(isOnBoardingDoneOnce = true)
        }

        // Plan: Onboarding Paywall — show on last slide of onboarding before home screen (first time user)
        // Key: splash_onboarding_paywall_screen_display from onboarding_paywall_config
        val shouldShowOnboardingPaywall = sharedPref.getAdExtraData(
            "onboarding_paywall_config",
            "splash_onboarding_paywall_screen_display"
        ) == "true"

        if (sharedPref.isFirstTimeUser && shouldShowOnboardingPaywall && !sharedPref.isAppPurchased) {
            // Plan: First time user → show onboarding paywall after theme screen
            findNavController().safeNav(
                currentDestId = R.id.themesFragment,
                actionId = R.id.action_themesFragment_to_onboardingPaywallFragment
            )
        } else {
            // Returning user OR paywall disabled OR already purchased → go to Main
            findNavController().safeNav(
                currentDestId = R.id.themesFragment,
                actionId = R.id.action_themesFragment_to_mainFragment
            )
        }
    }

    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply { putString("action_label", label) }
        mFirebaseAnalytics.logEvent(eventName, params)
    }

    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = getThemeColor(themeResId)
        val themeColorStateList = android.content.res.ColorStateList.valueOf(themeColor)
        binding?.apply {
            btnSelect.backgroundTintList = themeColorStateList
            btnLater.strokeColor = themeColorStateList
            btnLater.setTextColor(themeColor)
        }
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

    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()
        applyDynamicTheme(currentTheme)
    }

    private fun observeAdd() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModelEntrance.openAddState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                if (it) true.enableButton()
            }
        }
    }

    private fun Boolean.enableButton() {
        binding?.apply {
            btnSelect.isEnabled = this@enableButton
            btnSelect.alpha = if (this@enableButton) 1.0F else 0.5F
            btnLater.isEnabled = this@enableButton
            btnLater.alpha = if (this@enableButton) 1.0F else 0.5F
        }
    }
}






