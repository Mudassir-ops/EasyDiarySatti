package com.example.easydiarysatti.ui.onboarding.ongoingitem

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.ON_GOING_DATA_MODEL
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentOnGoingItemBinding
import com.example.easydiarysatti.parcelable
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding

class OnGoingItemFragment : Fragment(R.layout.fragment_on_going_item) {

    private val viewModel by viewModels<OnGoingItemViewModel>()
    private val binding by viewBinding(FragmentOnGoingItemBinding::bind)
    private var onGoingScreenUiModel: OnGoingScreenUiModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            try {
                // The way data is retrieved remains consistent to ensure
                // compatibility with the OnGoingPagerAdapter
                val loader = OnGoingScreenUiModel::class.java.classLoader
                arguments?.classLoader = loader
                onGoingScreenUiModel = arguments?.parcelable(ON_GOING_DATA_MODEL)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This line is crucial: it binds the new data (labels and images)
        // to the updated XML layout
        binding?.onBoardingModel = onGoingScreenUiModel
        if (onGoingScreenUiModel?.labelOne?.contains("Protect", ignoreCase = true) == true) {
            binding?.ivOnGoingItem?.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            // Keep original scaleType for others (usually fitCenter)
            binding?.ivOnGoingItem?.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }
}