package com.example.easydiarysatti.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.entrance.ViewModelEntrance
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.firebase.RemoteConfiguration
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentOnBoardingBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class OnBoardingFragment : Fragment(R.layout.fragment_on_boarding) {

    private var pagerAdapterRef: WeakReference<OnGoingPagerAdapter>? = null
    private val viewModel by viewModels<OnBoardingViewModel>()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    lateinit var mFirebaseAnalytics: FirebaseAnalytics

    @Inject lateinit var sharedPreferenceUtils: SharedPreferenceUtils
    @Inject lateinit var remoteConfiguration: RemoteConfiguration

    private val binding by viewBinding(FragmentOnBoardingBinding::bind)

    private val onGoingPagesList: Array<OnGoingScreenUiModel> by lazy {
        arrayOf(
            OnGoingScreenUiModel("Diary\nJournal", R.string.onboard_desc_1, R.drawable.on_board1),
            OnGoingScreenUiModel("Capture Your\nMemories", R.string.onboard_desc_2, R.drawable.on_board2),
            OnGoingScreenUiModel("Protect Your\nDiary Notes", R.string.onboard_desc_3, R.drawable.on_board3)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.viewModel = viewModel
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        setupOnGoingPagerAdapter()
        clickListeners()
        setupBannerObserver()

    }

    /* ---------- Banner Ad Implementation ---------- */
// 1. Define this variable at the top of your Fragment class (not inside the function)
//    private var isAdProcessStarted = false

    private fun setupBannerObserver() {
        if (!sharedPreferenceUtils.getAdShowStatus(BannerAdKey.ON_BOARDING.value)) {
            binding?.bannerShimmerContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            // 2. SAFETY LOCK: If we already started the timer for this screen, STOP here.
//            if (isAdProcessStarted) return@observe

            // Specifically grab the ad preloaded for this screen
            val preloadedAd = adMap[BannerAdKey.ON_BOARDING]

            if (preloadedAd != null) {
                // 3. ACTIVATE LOCK: Ensure this block only runs once per fragment lifecycle
//                isAdProcessStarted = true

                // Show the shimmer container and start animation
                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()

                // Check if fragment is still attached to avoid crashes
                if (isAdded && binding != null) {

                    // 5. TURN OFF SHIMMER: Stop animation and clear the effect
                    binding?.bannerShimmerContainer?.stopShimmer()
                    binding?.bannerShimmerContainer?.setShimmer(null)

                    binding?.bannerContainer?.let { container ->
                        // Remove gray background and show the ad
                        container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        container.addCleanView(preloadedAd)
                        container.visibility = View.VISIBLE
                    }
                    Log.d("AdDebug", "Shimmer off, ON_BOARDING Ad visible (One-time load)")
                }

            } else {
                // Keep container hidden while waiting for the ad to appear in the map
                binding?.bannerShimmerContainer?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
                Log.d("AdDebug", "ON_BOARDING ad not found in map yet...")
            }
        }
    }



    /* ---------- Waterfall Skip Logic ---------- */

    private fun navigateToNextScreen() {
        val showPermission = sharedPreferenceUtils.isPermissionEnabled
                && !sharedPreferenceUtils.isPermissionDone  // skip if already done/skipped
        val showWriting = sharedPreferenceUtils.isNameWritingEnabled
        val showPin = sharedPreferenceUtils.isPinSetupEnabled
        val showTheme = sharedPreferenceUtils.isThemeSelectionEnabled

        when {
            // Path 1: Permission screen — only if enabled AND not already done/skipped
            showPermission -> {
                preLoadNextBanner(BannerAdKey.PERMISSION)
                findNavController().safeNav(R.id.onBoardingFragment, R.id.action_onBoardingFragment_to_permissionFragment)
            }

            // Path 2: Skip Permission -> Check Start Writing
            showWriting -> {
                preLoadNextBanner(BannerAdKey.START_WRITING)
                findNavController().safeNav(R.id.onBoardingFragment, R.id.action_onBoardingFragment_to_nameSetupFragment)
            }

            // Path 3: Skip Permission & Writing -> Check Pin Setup
            showPin -> {
                preLoadNextBanner(BannerAdKey.PIN_SETUP)
                findNavController().safeNav(R.id.onBoardingFragment, R.id.action_onBoardingFragment_to_pinSetupFragment)
            }

            // Path 4: Skip All to Theme
            showTheme -> {
                preLoadNextBanner(BannerAdKey.THEME_SELECTION)
                findNavController().safeNav(R.id.onBoardingFragment, R.id.action_onBoardingFragment_to_themeFragment)
            }

            // Path 5: Everything Disabled -> Finish Setup and Go Home
            else -> {
                sharedPreferenceUtils.isFirstTimeUser = false
                findNavController().safeNav(R.id.onBoardingFragment, R.id.action_onBoardingFragment_to_mainFragment)
            }
        }
    }

    /**
     * Requirement: Ad calling from previous screen.
     * We load the ad here so the next fragment displays it immediately.
     */
    private fun preLoadNextBanner(adKey: BannerAdKey) {
        val adView = com.google.android.gms.ads.AdView(requireContext())
        bannerViewModel.loadBannerAd(adView, adKey, requireContext())
    }

    /* ---------- UI Logic ---------- */

    private fun setupOnGoingPagerAdapter() {
        val pagerAdapter = OnGoingPagerAdapter(childFragmentManager, lifecycle, onGoingPagesList)
        pagerAdapterRef = WeakReference(pagerAdapter)
        binding?.viewPagerEasyDiary?.apply {
            adapter = pagerAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding?.tvSkip?.visibility = if (position == onGoingPagesList.size - 1) View.INVISIBLE else View.VISIBLE
                    updateDotsIndicator(position)
                    updateProgressLoader(position)
                    logCurrentPageShow(position)
                }
            })
        }
    }

    private fun updateDotsIndicator(position: Int) {
        val dots = arrayOf(binding?.dot1, binding?.dot2, binding?.dot3)
        dots.forEachIndexed { index, imageView ->
            imageView?.setImageResource(if (index == position) R.drawable.ic_indicator_selected else R.drawable.ic_indicator_unselected)
        }
    }

    private fun updateProgressLoader(position: Int) {
        val progress = ((position + 1) * 34).coerceAtMost(100)
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
            tvSkip.setOnClickListener { navigateToNextScreen() }
        }
    }

    private fun logCurrentPageShow(position: Int) {
        val showKey = when (position) {
            0 -> "On_Boarding_Diary_journal"
            1 -> "On_Boarding_Capture_Your_Memories"
            2 -> "On_Boarding_Personal_&_Private"
            else -> ""
        }
        if (showKey.isNotEmpty()) logAnalyticsEvent(showKey, "page_show")
    }

    private fun logAnalyticsEvent(eventName: String, label: String) {
        val params = Bundle().apply { putString("action_label", label) }
        mFirebaseAnalytics.logEvent(eventName, params)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Stay on Onboarding */ }
        })
    }

    override fun onDestroyView() {
//        isAdProcessStarted = false
//        bannerViewModel.destroyBanner(BannerAdKey.ON_BOARDING)
        super.onDestroyView()
    }
}