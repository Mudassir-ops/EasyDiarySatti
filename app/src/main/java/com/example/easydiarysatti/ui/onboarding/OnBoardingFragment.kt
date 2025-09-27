package com.example.easydiarysatti.ui.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentOnBoardingBinding
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference


@AndroidEntryPoint
class OnBoardingFragment : Fragment(R.layout.fragment_on_boarding) {
    private var pagerAdapterRef: WeakReference<OnGoingPagerAdapter>? = null
    private val viewModel by viewModels<OnBoardingViewModel>()
    private val binding by viewBinding(FragmentOnBoardingBinding::bind)
    private val onGoingPagesList: Array<OnGoingScreenUiModel> by lazy {
        arrayOf(
            OnGoingScreenUiModel(
                labelOne = "Diary Journal",
                labelTwo = "Log your thoughts, track habits, and plan goals.",
                imageRes = R.drawable.intro_1,

                ), OnGoingScreenUiModel(
                labelOne = "Capture Your Memories",
                labelTwo = "Make your diary truly yours! Add tags or upload images.",
                imageRes = R.drawable.intro_2,
            ), OnGoingScreenUiModel(
                labelOne = "Personal & Private",
                labelTwo = "Preserve your private moments and make them wonderful.",
                imageRes = R.drawable.intro_3,
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.viewModel = viewModel
        setupOnGoingPagerAdapter()
        clickListeners()
    }

    private fun setupOnGoingPagerAdapter() {
        val pagerAdapter = OnGoingPagerAdapter(
            childFragmentManager, lifecycle = lifecycle, onGoingPagesList = onGoingPagesList
        )
        pagerAdapterRef = WeakReference(pagerAdapter)
        binding?.viewPagerEasyDiary?.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false
        }
    }

    private fun clickListeners() {
        binding?.apply {

        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

}