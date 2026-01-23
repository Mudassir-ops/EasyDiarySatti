package com.example.easydiarysatti.ui.theme

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_ONBOARDING
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.databinding.FragmentThemesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ThemesFragment : Fragment(R.layout.fragment_themes) {
    private val viewModel by viewModels<ThemesViewModel>()
    private val viewModelEntrance by viewModels<ViewModelEntrance>()
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val binding by viewBinding(FragmentThemesBinding::bind)
    private var themeAdapter: ThemeAdapter? = null
    private val themesList: List<Int> by lazy {
        listOf(
            R.drawable.theme_2,
            R.drawable.theme_1,
            R.drawable.theme_3,
            R.drawable.theme_4,
            R.drawable.theme_5,
        )
    }

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    @Inject
    lateinit var appOpenAdsConfig: AppOpenAdsConfig





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        logAnalyticsEvent("On_Boarding_Choose_Your_Theme","open_screen")

        themeAdapter = ThemeAdapter(themes = themesList, onThemeClick = {})
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        observeAdd()
        setupBgTheme()
//        if (arguments?.getBoolean(FROM_ONBOARDING) == true) {
//            loadAppOpen()
//        }
        binding?.apply {
            themeViewPager.adapter = themeAdapter
            themeViewPager.offscreenPageLimit = 4
            themeViewPager.setPageTransformer { page, position ->
                page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
            }
            themeViewPager.setCurrentItem(1, false)
        }
    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply {
            putString("action_label", label)
        }
        mFirebaseAnalytics.logEvent(eventName, params)
    }
    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = getThemeColor(themeResId)
        val themeColorStateList = android.content.res.ColorStateList.valueOf(themeColor)

        binding?.apply {
            // 1. Filled Button (btnSelect)
            // We update the background tint so the whole button takes the theme color
            btnSelect.backgroundTintList = themeColorStateList

            // 2. Unselected/Outline Button (btnLater)
            // Since you have app:backgroundTint="@null", we update the Stroke (border)
            // and the Text color to match the theme.
            btnLater.strokeColor = themeColorStateList
            btnLater.setTextColor(themeColor)

            // ... (Keep your existing logic for btnNext, dots, etc.)
        }
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
    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()
        applyDynamicTheme(currentTheme)
    }
    private fun observeAdd() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModelEntrance.openAddState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                if (it) {
                    true.enableButton()
                }
            }
        }
    }

    private fun clickListener() {
        binding?.apply {
            btnSelect.setOnClickListener {
                logAnalyticsEvent("On_Boarding_Choose_Your_Theme_Select","select_click")

                val currentPosition = binding?.themeViewPager?.currentItem ?: 0
                val selectedThemeResId = themesList[currentPosition]
                sessionManagerRepo.setBgTheme(themeResId = selectedThemeResId)
                moveToNextScreen()
            }
            btnLater.setOnClickListener {
                logAnalyticsEvent("On_Boarding_Choose_Your_Theme_Later","later_click")
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
        findNavController().safeNav(
            currentDestId = R.id.themesFragment,
            actionId = R.id.action_themesFragment_to_mainFragment
        )
    }

    private fun loadAppOpen() {
        false.enableButton()
        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.THEME, object : AppOpenOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean, errorMessage: String?) {
                if (successfullyLoaded) {
                    appOpenAdsConfig.showAppOpenAd(activity ?: return, AppOpenAdKey.THEME, object :
                        AppOpenOnShowCallBack {
                        override fun onAdDismissedFullScreenContent() {
                            onAppOpenResponse()
                        }

                        override fun onAdFailedToShow() {
                            onAppOpenResponse()
                        }

                        override fun onAdClicked() {}
                        override fun onAdShowedFullScreenContent() {}
                        override fun onAdImpression() {}
                        override fun onAdImpressionDelayed() {}
                    })
                } else {
                    onAppOpenResponse()
                }
            }
        })
    }

    private fun onAppOpenResponse() {
        viewModelEntrance.onAdResponse()
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