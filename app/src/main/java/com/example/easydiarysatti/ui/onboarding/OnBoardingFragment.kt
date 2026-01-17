package com.example.easydiarysatti.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.firebase.RemoteConfiguration
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.databinding.FragmentOnBoardingBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class OnBoardingFragment : Fragment(R.layout.fragment_on_boarding) {
    private var pagerAdapterRef: WeakReference<OnGoingPagerAdapter>? = null
    private val viewModel by viewModels<OnBoardingViewModel>()
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val viewModelEntrance by viewModels<ViewModelEntrance>()

    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils

    @Inject
    lateinit var remoteConfiguration: RemoteConfiguration

    private val binding by viewBinding(FragmentOnBoardingBinding::bind)

    private val onGoingPagesList: Array<OnGoingScreenUiModel> by lazy {
        arrayOf(
            OnGoingScreenUiModel(
                labelOne = "Diary\nJournal",
                labelTwo =R.string.onboard_desc_1,
                imageRes = R.drawable.on_board1
            ),
            OnGoingScreenUiModel(
                labelOne = "Capture Your\nMemories",
                labelTwo = R.string.onboard_desc_2,
                imageRes = R.drawable.on_board2
            ),
            OnGoingScreenUiModel(
                labelOne = "Protect Your\nDiary Notes",
                labelTwo = R.string.onboard_desc_3,
                imageRes = R.drawable.on_board3
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

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    // 1. Hide Skip on the last page (index 2 for 3 pages)
                    if (position == onGoingPagesList.size - 1) {
                        binding?.tvSkip?.visibility = View.INVISIBLE
                    } else {
                        binding?.tvSkip?.visibility = View.VISIBLE
                    }

                    // 2. Update Manual Dots Width and Source
                    updateDotsIndicator(position)

                    // 3. Update Circular Progress
                    updateProgressLoader(position)

                    logCurrentPageShow(position)
                }
            })
        }
    }

    private fun updateDotsIndicator(position: Int) {


        val dots = arrayOf(binding?.dot1, binding?.dot2, binding?.dot3)

        dots.forEachIndexed { index, imageView ->
            val params = imageView?.layoutParams
            if (index == position) {
                // Selected state: Wide Pill
//                params?.width = selectedWidth
                imageView?.setImageResource(R.drawable.ic_indicator_selected) // Your selected SVG
            } else {
                // Unselected state: Small Circle
//                params?.width = unselectedWidth
                imageView?.setImageResource(R.drawable.ic_indicator_unselected) // Your unselected SVG
            }
            imageView?.layoutParams = params
        }
    }

    private fun updateProgressLoader(position: Int) {
        val progress = when (position) {
            0 -> 33
            1 -> 66
            2 -> 100
            else -> 0
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            binding?.onboardingProgress?.setProgress(progress, true)
        } else {
            binding?.onboardingProgress?.progress = progress
        }
    }

    private fun clickListeners() {
        binding?.apply {
            btnNext.setOnClickListener {
                val nextItem = viewPagerEasyDiary.currentItem + 1
                if (nextItem < onGoingPagesList.size) {
                    viewPagerEasyDiary.currentItem = nextItem
                } else {
                    navigateToNextScreen()
                }
            }

            tvSkip.setOnClickListener {
                navigateToNextScreen()
            }
        }
    }

    private fun navigateToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.onBoardingFragment,
            actionId = R.id.action_onBoardingFragment_to_permissionFragment
        )
    }

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
                // Handle back press if needed
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    private fun initRemoteConfigs() {
        remoteConfiguration.checkRemoteConfig { viewModelEntrance.onRemoteConfigResponse() }
    }

    private fun initObservers() {
        viewModelEntrance.remoteConfigResponseLiveData.observe(viewLifecycleOwner) {
            // Observe remote config if necessary
        }
    }
}