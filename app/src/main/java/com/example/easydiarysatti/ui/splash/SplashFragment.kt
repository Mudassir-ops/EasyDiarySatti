package com.example.easydiarysatti.ui.splash
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.firebase.RemoteConfiguration
import com.example.easydiarysatti.ads.interstitial.InterstitialAdsConfig
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnLoadCallBack
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.interstitial.enums.InterAdKey
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.getValue


@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()

    private var isNavigatedInternal = false
    private val TAG_ADS = "AdsInformation"

    @Inject lateinit var appOpenAdsConfig: AppOpenAdsConfig
    @Inject lateinit var interAdsConfig: InterstitialAdsConfig
    @Inject lateinit var internetManager: InternetManager
    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var remoteConfig: RemoteConfiguration

    private var timeoutJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                viewLifecycleOwner.lifecycle.whenStarted {
                    Log.d(TAG_ADS, "SplashState Observed: $state")
                    when (state) {
                        is SplashState.ShowAd -> fetchConfigAndStartAds()
                        // This handles the new "Controlled Flow" logic
                        is SplashState.NavigateToOnboarding -> handleControlledNavigation()
                        is SplashState.NavigateToLogin -> {



                            // Now navigate to the Login/Home screen
                            navigateTo(R.id.action_splashFragment_to_loginFragment)
                        }
                        is SplashState.Idle -> Unit
                    }
                }
            }
        }


        viewModel.startLogic(internetManager.isInternetConnected)

    }

    private fun handleControlledNavigation() {

        when {
            // 1. Check Onboarding
            sharedPref.isOnboardingEnabled -> {
                preLoadBanner(BannerAdKey.ON_BOARDING)
                navigateTo(R.id.action_splashFragment_to_onBoardingFragment)
            }

            // 2. Check Permission (If Onboarding skipped)
            sharedPref.isPermissionEnabled -> {
                preLoadBanner(BannerAdKey.PERMISSION)
                navigateTo(R.id.action_splashFragment_to_permissionFragment)
            }

            // 3. Check Start Writing (If Onboarding & Permission skipped)
            sharedPref.isNameWritingEnabled -> {
                preLoadBanner(BannerAdKey.START_WRITING)
                navigateTo(R.id.action_splashFragment_to_nameSetupFragment)
            }

            // 4. Check Pin Setup
            sharedPref.isPinSetupEnabled -> {
                preLoadBanner(BannerAdKey.PIN_SETUP)
                navigateTo(R.id.action_splashFragment_to_pinSetupFragment)
            }

            // 5. Check Theme Selection
            sharedPref.isThemeSelectionEnabled -> {
                preLoadBanner(BannerAdKey.THEME_SELECTION)
                navigateTo(R.id.action_splashFragment_to_themeFragment)
            }

            // 6. All Skips -> Go to Home
            else -> {
                sharedPref.isFirstTimeUser = false
                navigateTo(R.id.action_splashFragment_to_mainFragment)
            }
        }
    }

    private fun preLoadBanner(adKey: BannerAdKey) {
        Log.d(TAG_ADS, "Ad Calling from Splash for: ${adKey.value}")
        val dummyAdView = AdView(requireContext())
        bannerViewModel.loadBannerAd(dummyAdView, adKey, requireContext())
    }

    private fun fetchConfigAndStartAds() {
        Log.d(TAG_ADS, "Step 1: Fetching Remote Config...")

        // Use the dynamic value from SharedPreferences
        val dynamicTimeout = sharedPref.splashTimeout

        timeoutJob = lifecycleScope.launch {
            delay(dynamicTimeout)
            if (!isNavigatedInternal) {
                Log.d(TAG_ADS, "Splash Timeout reached after $dynamicTimeout ms. Moving to next screen.")
                viewModel.onAdFinished()
            }
        }

        remoteConfig.checkRemoteConfig { success ->
            lifecycleScope.launch(Dispatchers.Main) {
                if (success) startAdFlow() else viewModel.onAdFinished()
            }
        }
    }
    private fun startAdFlow() {
        val isFirstTime = sharedPref.isFirstTimeUser
        val interKey = if (isFirstTime) InterAdKey.SPLASH_INTER_FIRST_TIME else InterAdKey.SPLASH_INTER_RETURN_USER
        val appOpenKey = if (isFirstTime) AppOpenAdKey.SPLASH_FIRST_TIME else AppOpenAdKey.SPLASH_RETURN_USER

        if (sharedPref.getAdShowStatus(interKey.value)) {
            loadInterstitial(interKey, appOpenKey)
        } else if (sharedPref.getAdShowStatus(appOpenKey.value)) {
            loadAppOpen(appOpenKey)
        } else {
            viewModel.onAdFinished()
        }
    }

    private fun loadInterstitial(adKey: InterAdKey, fallbackKey: AppOpenAdKey) {
        interAdsConfig.loadInterstitialAd(adKey, object : InterstitialOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean) {
                if (successfullyLoaded && isAdded) {
                    timeoutJob?.cancel()
                    interAdsConfig.showInterstitialWithDialog(requireActivity(), adKey, object : InterstitialOnShowCallBack {
                        override fun onAdDismissedFullScreenContent() { viewModel.onAdFinished() }
                        override fun onAdFailedToShow() { viewModel.onAdFinished() }
                    })
                } else {
                    loadAppOpen(fallbackKey)
                }
            }
        })
    }

    private fun loadAppOpen(adKey: AppOpenAdKey) {
        // Safety check: is the specific Splash App Open enabled?
        if (!sharedPref.getAdShowStatus(adKey.value)) {
            viewModel.onAdFinished()
            return
        }

        appOpenAdsConfig.loadAppOpenAd(adKey, object : AppOpenOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean, errorMessage: String?) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (successfullyLoaded && isAdded && activity != null) {
                        timeoutJob?.cancel()

                        // This calls showAppOpen(activity, adKey.value)
                        // It will pull from the Map using the Splash key!
                        appOpenAdsConfig.showAppOpenAd(requireActivity(), adKey, object : AppOpenOnShowCallBack {
                            override fun onAdDismissedFullScreenContent() {
                                viewModel.onAdFinished()
                            }
                            override fun onAdFailedToShow() {
                                viewModel.onAdFinished()
                            }
                        })
                    } else {
                        // If App Open fails to load, go to Home
                        viewModel.onAdFinished()
                    }
                }
            }
        })
    }
    private fun navigateTo(actionId: Int) {
        if (isNavigatedInternal || !isAdded) return
        try {
            isNavigatedInternal = true
            timeoutJob?.cancel()
            findNavController().navigate(actionId)
        } catch (e: Exception) {
            Log.e(TAG_ADS, "Navigation Error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeoutJob?.cancel()
    }
}