package com.example.easydiarysatti.ui.splash

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ui.login.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.whenStarted

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val viewModel: SplashViewModel by viewModels()
    private var isNavigatedInternal = false

    @Inject lateinit var appOpenAdsConfig: AppOpenAdsConfig
    @Inject lateinit var internetManager: InternetManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                // Use whenStarted inside the collect to ensure we are in a safe state
                viewLifecycleOwner.lifecycle.whenStarted {
                    when (state) {
                        is SplashState.ShowAd -> startAdFlow()
                        is SplashState.NavigateToOnboarding -> navigateTo(R.id.action_splashFragment_to_onBoardingFragment)
                        is SplashState.NavigateToLogin -> navigateTo(R.id.loginFragment)
                        is SplashState.Idle -> Unit
                    }
                }
            }
        }

        viewModel.startLogic(internetManager.isInternetConnected)
    }

    private fun startAdFlow() {
        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.NAME_SCREEN, object : AppOpenOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean, errorMessage: String?) {
                if (successfullyLoaded && isAdded) {
                    showAd()
                } else {
                    viewModel.onAdFinished()
                }
            }
        })
    }

    private fun showAd() {
        val activity = activity ?: return
        appOpenAdsConfig.showAppOpenAd(activity, AppOpenAdKey.NAME_SCREEN,
            object : AppOpenOnShowCallBack {
                override fun onAdDismissedFullScreenContent() {
                    // 2. ONLY proceed if the fragment is still attached to the activity
                    if (isAdded && !isDetached) {
                        viewModel.onAdFinished()
                    }
                }
                override fun onAdFailedToShow() {
                    if (isAdded && !isDetached) {
                        viewModel.onAdFinished()
                    }
                }
                override fun onAdClicked() {}
                override fun onAdShowedFullScreenContent() {}
                override fun onAdImpression() {}
                override fun onAdImpressionDelayed() {}
            })
    }

    private fun navigateTo(actionId: Int) {
        // 1. Safety check: Ensure fragment is attached and view exists
        if (isNavigatedInternal || !isAdded || view == null) return

        try {
            val navController = findNavController()
            isNavigatedInternal = true
            navController.navigate(actionId)
        } catch (e: Exception) {
            Log.e("Splash", "Navigation failed: ${e.message}")
        }
    }
}