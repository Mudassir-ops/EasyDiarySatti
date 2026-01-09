package com.example.easydiarysatti.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.firebase.RemoteConfiguration
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.databinding.FragmentOnBoardingBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject


@AndroidEntryPoint
class OnBoardingFragment : Fragment(R.layout.fragment_on_boarding) {
    private var pagerAdapterRef: WeakReference<OnGoingPagerAdapter>? = null
    private val viewModel by viewModels<OnBoardingViewModel>()
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val viewModelEntrance by viewModels<ViewModelEntrance>()

    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils

    @Inject
    lateinit var remoteConfiguration: RemoteConfiguration

    private val binding by viewBinding(FragmentOnBoardingBinding::bind)
    private val onGoingPagesList: Array<OnGoingScreenUiModel> by lazy {
        arrayOf(
            OnGoingScreenUiModel(
                labelOne = "Diary Journal",
                labelTwo = "Log your thoughts, track habits, and\nplan goals.",
                imageRes = R.drawable.intro_new1,

                ), OnGoingScreenUiModel(
                labelOne = "Capture Your Memories",
                labelTwo = "Make your diary truly yours! Add tags\nor upload images.",
                imageRes = R.drawable.intro_new2,
            ), OnGoingScreenUiModel(
                labelOne = "Personal & Private",
                labelTwo = "Preserve your private moments and\nmake them wonderful.",
                imageRes = R.drawable.intro_new3,
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.viewModel = viewModel
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        setupOnGoingPagerAdapter()
        clickListeners()
        initRemoteConfigs()
        initObservers()
    }

    private fun setupOnGoingPagerAdapter() {
        val pagerAdapter = OnGoingPagerAdapter(
            childFragmentManager, lifecycle = lifecycle, onGoingPagesList = onGoingPagesList
        )
        pagerAdapterRef = WeakReference(pagerAdapter)
        binding?.viewPagerEasyDiary?.apply {
            adapter = pagerAdapter
        }
    }

    private fun clickListeners() {
        binding?.apply {
            btnNext.setOnClickListener {
                // Determine which "Next" key to use based on current page
                val nextKey = when (viewPagerEasyDiary.currentItem) {
                    0 -> "On_Boarding_Diary_journal_Next"
                    1 -> "On_Boarding_Capture_Your_Memories_Next"
                    2 -> "On_Boarding_Personal_&_Private_Next"
                    else -> ""
                }
                logAnalyticsEvent(nextKey, "click_next")

                val nextItem = viewPagerEasyDiary.currentItem + 1
                if (nextItem < (viewPagerEasyDiary.adapter?.itemCount ?: 0)) {
                    viewPagerEasyDiary.currentItem = nextItem

                    // Log the "Show" event for the NEW page the user just moved to
                    logCurrentPageShow(nextItem)
                } else {
                    findNavController().safeNav(
                        currentDestId = R.id.onBoardingFragment,
                        actionId = R.id.action_onBoardingFragment_to_permissionFragment
                    )
                }
            }

            tvSkip.setOnClickListener {
                // Determine which "Skip" key to use based on current page
                val skipKey = when (viewPagerEasyDiary.currentItem) {
                    0 -> "On_Boarding_Diary_journal_Skip"
                    1 -> "On_Boarding_Capture_Your_Memories_Skip"
                    2 -> "On_Boarding_Personal_&_Private_Skip"
                    else -> ""
                }
                logAnalyticsEvent(skipKey, "click_skip")

                findNavController().safeNav(
                    currentDestId = R.id.onBoardingFragment,
                    actionId = R.id.action_onBoardingFragment_to_permissionFragment
                )
            }
        }
    }
    // Helper to log the initial "Show" event for each page
    private fun logCurrentPageShow(position: Int) {
        val showKey = when (position) {
            0 -> "On_Boarding_Diary_journal"
            1 -> "On_Boarding_Capture_Your_Memories"
            2 -> "On_Boarding_Personal_&_Private"
            else -> ""
        }
        if (showKey.isNotEmpty()) {
            logAnalyticsEvent(showKey, "page_show")
        }
    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply {
            putString("action_label", label)
        }
        mFirebaseAnalytics.logEvent(eventName, params)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    private fun initRemoteConfigs() {
        remoteConfiguration.checkRemoteConfig { viewModelEntrance.onRemoteConfigResponse() }
    }

    private fun initObservers() {
        viewModelEntrance.remoteConfigResponseLiveData.observe(viewLifecycleOwner) {
            // val appOpen = "appOpen"
            //val bannerHome = "bannerHome"
            //val interFeature = "interFeature"
            //val rewardedInterAiFeature = "rewardedInterAiFeature"
        }
    }

}