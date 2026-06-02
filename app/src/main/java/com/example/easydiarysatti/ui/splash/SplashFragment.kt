package com.example.easydiarysatti.ui.splash

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
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
import com.example.easydiarysatti.databinding.FragmentSplashBinding
import com.example.easydiarysatti.viewBinding
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    private val binding by viewBinding(FragmentSplashBinding::bind)

    private var isNavigatedInternal = false
    private val TAG_ADS = "AdsInformation"

    @Inject lateinit var appOpenAdsConfig: AppOpenAdsConfig
    @Inject lateinit var interAdsConfig: InterstitialAdsConfig
    @Inject lateinit var internetManager: InternetManager
    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var remoteConfig: RemoteConfiguration

    private var timeoutJob: Job? = null
    private var splashTimer: CountDownTimer? = null

    private var adReadyToShow: Boolean = false
    private var timerFinished: Boolean = false

    // Navigation action decided by the ViewModel while the progress bar is running.
    // Stored here so startProgressBar.onFinish() can fire it — ensuring the bar
    // always completes fully before leaving splash, even with no internet.
    private var pendingNavigation: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appPrefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!appPrefs.getBoolean("app_launched_before", false)) {
            sharedPref.isFirstTimeUser = true
            appPrefs.edit().putBoolean("app_launched_before", true).apply()
            Log.d(TAG_ADS, "First launch — isFirstTimeUser = true")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                viewLifecycleOwner.lifecycle.whenStarted {
                    Log.d(TAG_ADS, "SplashState Observed: $state")
                    when (state) {
                        is SplashState.ShowAd -> fetchConfigAndStartAds()

                        is SplashState.NavigateToOnboarding -> {
                            startProgressBar()
                            pendingNavigation = { handleControlledNavigation() }
                            if (timerFinished) firePendingNavigation()
                        }

                        is SplashState.NavigateToLogin -> {
                            startProgressBar()
                            pendingNavigation = {
                                val shouldShowSplash = sharedPref.shouldShowSplashPaywall()
                                if (!sharedPref.isFirstTimeUser && shouldShowSplash) {
                                    navigateTo(R.id.action_splashFragment_to_splashPaywallFragment)
                                } else {
                                    navigateTo(R.id.action_splashFragment_to_loginFragment)
                                }
                            }
                            if (timerFinished) firePendingNavigation()
                        }

                        is SplashState.Idle -> Unit
                    }
                }
            }
        }

        viewModel.startLogic(internetManager.isInternetConnected)
    }

    /**
     * HOW THE PROGRESS BAR + AD FLOW WORKS TOGETHER:
     *
     * The progress bar timer and the ad loading run IN PARALLEL — they start at the same time.
     * This ensures the 5-second bar is always smooth regardless of network speed.
     *
     * Two things must both be true before the ad shows:
     *   1. timerFinished = true  (5-second bar completed)
     *   2. adReadyToShow = true  (remote config fetched + ad loaded)
     *
     * Whichever finishes last triggers the show. This means:
     *   - Fast network: ad loads in 1s, timer finishes at 5s → show at 5s ✓
     *   - Slow network: timer finishes at 5s, ad loads at 7s → show at 7s ✓
     *   - Ad fails:     adReadyToShow never set → timeout job navigates away ✓
     *
     * The timeout job (splashTimeout ms) is the final safety net — if the ad never
     * loads, we don't wait forever.
     */
    private fun fetchConfigAndStartAds() {
        Log.d(TAG_ADS, "Step 1: Starting progress bar + ad load in parallel")

        adReadyToShow = false
        timerFinished = false

        startProgressBar()

        val dynamicTimeout = sharedPref.splashTimeout
        timeoutJob = lifecycleScope.launch {
            delay(dynamicTimeout)
            if (!isNavigatedInternal) {
                Log.d(TAG_ADS, "Splash timeout ($dynamicTimeout ms) — navigating away")
                splashTimer?.cancel()
                viewModel.onAdFinished()
            }
        }

        if (!internetManager.isInternetConnected) {
            Log.d(TAG_ADS, "No internet — skipping ads, waiting for progress bar")
            adReadyToShow = true
            pendingAdShow = { viewModel.onAdFinished() }
            if (timerFinished) showPendingAd()
            return
        }

        remoteConfig.checkRemoteConfig { success ->
            lifecycleScope.launch(Dispatchers.Main) {
                if (success) {
                    startAdFlow()
                } else {
                    Log.d(TAG_ADS, "Remote config failed — skipping ads")
                    adReadyToShow = true
                    pendingAdShow = { viewModel.onAdFinished() }
                    if (timerFinished) showPendingAd()
                }
            }
        }
    }

    private fun startProgressBar() {
        val bar = binding?.splashProgressBar ?: return
        bar.progress = 0
        bar.visibility = View.VISIBLE

        splashTimer = object : CountDownTimer(5000, 50) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = 5000 - millisUntilFinished
                val progressPercentage = (elapsed / 50).toInt()
                bar.progress = progressPercentage * 10
            }

            override fun onFinish() {
                bar.progress = 1000
                timerFinished = true
                Log.d(TAG_ADS, "Progress bar done. adReadyToShow=$adReadyToShow pendingNavigation=${pendingNavigation != null}")

                when {
                    adReadyToShow      -> showPendingAd()
                    pendingNavigation != null -> firePendingNavigation()
                }
            }
        }.start()
    }

    private fun firePendingNavigation() {
        pendingNavigation?.invoke()
        pendingNavigation = null
    }

    private var pendingAdShow: (() -> Unit)? = null

    private fun showPendingAd() {
        timeoutJob?.cancel()
        pendingAdShow?.invoke()
        pendingAdShow = null
    }

    private fun startAdFlow() {
        val isFirstTime = sharedPref.isFirstTimeUser
        val interKey   = if (isFirstTime) InterAdKey.SPLASH_INTER_FIRST_TIME else InterAdKey.SPLASH_INTER_RETURN_USER
        val appOpenKey = if (isFirstTime) AppOpenAdKey.SPLASH_FIRST_TIME else AppOpenAdKey.SPLASH_RETURN_USER

        if (sharedPref.getAdShowStatus(interKey.value)) {
            loadInterstitial(interKey, appOpenKey)
        } else if (sharedPref.getAdShowStatus(appOpenKey.value)) {
            loadAppOpen(appOpenKey)
        } else {
            adReadyToShow = true
            pendingAdShow = { viewModel.onAdFinished() }
            if (timerFinished) showPendingAd()
        }
    }

    private fun loadInterstitial(adKey: InterAdKey, fallbackKey: AppOpenAdKey) {
        interAdsConfig.loadInterstitialAd(adKey, object : InterstitialOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean) {
                if (successfullyLoaded && isAdded) {
                    adReadyToShow = true
                    pendingAdShow = {
                        interAdsConfig.showInterstitialWithDialog(requireActivity(), adKey, object : InterstitialOnShowCallBack {
                            override fun onAdDismissedFullScreenContent() { viewModel.onAdFinished() }
                            override fun onAdFailedToShow() { viewModel.onAdFinished() }
                        })
                    }
                    Log.d(TAG_ADS, "Interstitial ready. timerFinished=$timerFinished")
                    if (timerFinished) showPendingAd()
                } else {
                    loadAppOpen(fallbackKey)
                }
            }
        })
    }

    private fun loadAppOpen(adKey: AppOpenAdKey) {
        if (!sharedPref.getAdShowStatus(adKey.value)) {
            adReadyToShow = true
            pendingAdShow = { viewModel.onAdFinished() }
            if (timerFinished) showPendingAd()
            return
        }
        appOpenAdsConfig.loadAppOpenAd(adKey, object : AppOpenOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean, errorMessage: String?) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (successfullyLoaded && isAdded && activity != null) {
                        adReadyToShow = true
                        pendingAdShow = {
                            appOpenAdsConfig.showAppOpenAd(requireActivity(), adKey, object : AppOpenOnShowCallBack {
                                override fun onAdDismissedFullScreenContent() { viewModel.onAdFinished() }
                                override fun onAdFailedToShow() { viewModel.onAdFinished() }
                            })
                        }
                        Log.d(TAG_ADS, "AppOpen ready. timerFinished=$timerFinished")
                        if (timerFinished) showPendingAd()
                    } else {
                        adReadyToShow = true
                        pendingAdShow = { viewModel.onAdFinished() }
                        if (timerFinished) showPendingAd()
                    }
                }
            }
        })
    }

    /**
     * Decides where to navigate after splash for a FIRST-TIME user.
     *
     * Instead of checking individual boolean flags, we read the active first-open
     * flow from the JSON and navigate to the very first screen in that list.
     * Each subsequent screen is responsible for reading nextScreenAfter() to
     * continue the waterfall.
     *
     * JSON example (active_case = "case_full"):
     *   flow = ["onboarding_slides", "permission", "name", "pin", "theme", "home"]
     *   → navigate to onboarding_slides
     *
     * JSON example (active_case = "case_no_permission"):
     *   flow = ["onboarding_slides", "name", "pin", "theme", "home"]
     *   → navigate to onboarding_slides  (permission is simply absent from the list)
     */
    private fun handleControlledNavigation() {
        val flow = sharedPref.getFirstOpenScreenFlow()
        Log.d(TAG_ADS, "Onboarding flow: $flow")

        // Walk the flow until we find the first actionable screen
        for (screen in flow) {
            when (screen) {
                SharedPreferenceUtils.SCREEN_ONBOARDING_SLIDES -> {
                    preLoadBanner(BannerAdKey.ON_BOARDING)
                    navigateTo(R.id.action_splashFragment_to_onBoardingFragment)
                    return
                }
                SharedPreferenceUtils.SCREEN_PERMISSION -> {
                    preLoadBanner(BannerAdKey.PERMISSION)
                    navigateTo(R.id.action_splashFragment_to_permissionFragment)
                    return
                }
                SharedPreferenceUtils.SCREEN_NAME -> {
                    preLoadBanner(BannerAdKey.START_WRITING)
                    navigateTo(R.id.action_splashFragment_to_nameSetupFragment)
                    return
                }
                SharedPreferenceUtils.SCREEN_PIN -> {
                    preLoadBanner(BannerAdKey.PIN_SETUP)
                    navigateTo(R.id.action_splashFragment_to_pinSetupFragment)
                    return
                }
                SharedPreferenceUtils.SCREEN_THEME -> {
                    preLoadBanner(BannerAdKey.THEME_SELECTION)
                    navigateTo(R.id.action_splashFragment_to_themeFragment)
                    return
                }
                SharedPreferenceUtils.SCREEN_HOME -> {
                    // "home" is the terminal node — no more onboarding screens
                    val shouldShowOnboardingPaywall = sharedPref.getAdExtraData(
                        "onboarding_paywall_config",
                        "splash_onboarding_paywall_screen_display"
                    ) == "true"
                    if (sharedPref.isFirstTimeUser && shouldShowOnboardingPaywall && !sharedPref.isAppPurchased) {
                        navigateTo(R.id.action_splashFragment_to_onboardingPaywallFragment)
                    } else {
                        sharedPref.isFirstTimeUser = false
                        navigateTo(R.id.action_splashFragment_to_mainFragment)
                    }
                    return
                }
            }
        }

        // Fallback: should never reach here, but navigate home to be safe
        sharedPref.isFirstTimeUser = false
        navigateTo(R.id.action_splashFragment_to_mainFragment)
    }

    private fun preLoadBanner(adKey: BannerAdKey) {
        Log.d(TAG_ADS, "Ad Calling from Splash for: ${adKey.value}")
        val dummyAdView = AdView(requireContext())
        bannerViewModel.loadBannerAd(dummyAdView, adKey, requireContext())
    }

    private fun navigateTo(actionId: Int) {
        if (isNavigatedInternal || !isAdded) return
        try {
            isNavigatedInternal = true
            timeoutJob?.cancel()
            splashTimer?.cancel()
            findNavController().navigate(actionId)
        } catch (e: Exception) {
            Log.e(TAG_ADS, "Navigation Error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeoutJob?.cancel()
        splashTimer?.cancel()
    }
}