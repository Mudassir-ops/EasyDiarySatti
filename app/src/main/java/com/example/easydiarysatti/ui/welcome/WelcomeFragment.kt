package com.example.easydiarysatti.ui.welcome

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnLoadCallBack
import com.example.easydiarysatti.ads.appOpen.screen.callbacks.AppOpenOnShowCallBack
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.databinding.FragmentWelcomeBinding
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    private val viewModel by viewModels<WelcomeViewModel>()
    private val binding by viewBinding(FragmentWelcomeBinding::bind)
    private val viewModelEntrance by viewModels<ViewModelEntrance>()

    @Inject
    lateinit var appOpenAdsConfig: AppOpenAdsConfig
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            txtWelcomeBack.text = getString(R.string.welcome_back, viewModel.getName())
            imgIntroOne.loadImage(resourceId = R.drawable.name_pic)
            clickListeners()
        }
        loadAppOpen()
        observeAdd()
    }

    private fun clickListeners() {
        binding?.apply {
            btnNext.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.welcomeFragment,
            actionId = R.id.action_welcomeFragment_to_mainFragment
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }


    private fun loadAppOpen() {
        false.enableButton()
        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.NAME_SCREEN, object : AppOpenOnLoadCallBack {
            override fun onResponse(successfullyLoaded: Boolean, errorMessage: String?) {
                if (successfullyLoaded) {
                    appOpenAdsConfig.showAppOpenAd(
                        activity ?: return,
                        AppOpenAdKey.NAME_SCREEN,
                        object :
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
            btnNext.isEnabled = this@enableButton
            btnNext.alpha = if (this@enableButton) 1.0F else 0.5F
        }
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
}