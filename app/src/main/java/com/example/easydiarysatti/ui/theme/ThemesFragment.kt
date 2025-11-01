package com.example.easydiarysatti.ui.theme

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_ONBOARDING
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.databinding.FragmentThemesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ThemesFragment : Fragment(R.layout.fragment_themes) {
    private val viewModel by viewModels<ThemesViewModel>()
    private val viewModelEntrance by viewModels<ViewModelEntrance>()
    private val binding by viewBinding(FragmentThemesBinding::bind)
    private var themeAdapter: ThemeAdapter? = null
    private val themesList: List<Int> by lazy {
        listOf(
            R.drawable.theme_2,
            R.drawable.theme_1,
            R.drawable.theme_3,
            R.drawable.theme_4,
        )
    }

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo


    @Inject
    lateinit var appOpenAdsConfig: AppOpenAdsConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeAdapter = ThemeAdapter(themes = themesList, onThemeClick = {})
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        observeAdd()
        loadAppOpen()
        binding?.apply {
            themeViewPager.adapter = themeAdapter
            themeViewPager.offscreenPageLimit = 4
            themeViewPager.setPageTransformer { page, position ->
                page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
            }
            themeViewPager.setCurrentItem(1, false)
        }
    }

    private fun observeAdd() {
        viewModelEntrance.navigateLiveData.observe(viewLifecycleOwner) {
            true.enableButton()
        }
    }

    private fun clickListener() {
        binding?.apply {
            btnSelect.setOnClickListener {
                val currentPosition = binding?.themeViewPager?.currentItem ?: 0
                val selectedThemeResId = themesList[currentPosition]
                sessionManagerRepo.setBgTheme(themeResId = selectedThemeResId)
                moveToNextScreen()
            }
            btnLater.setOnClickListener { moveToNextScreen() }
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