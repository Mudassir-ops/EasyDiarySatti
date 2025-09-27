package com.example.easydiarysatti.ui.onboarding.ongoingitem


import android.os.Bundle
import android.view.View
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
            onGoingScreenUiModel = arguments?.parcelable(ON_GOING_DATA_MODEL)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.onBoardingModel = onGoingScreenUiModel
    }

}