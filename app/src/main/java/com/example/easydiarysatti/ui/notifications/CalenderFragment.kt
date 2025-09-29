package com.example.easydiarysatti.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentCalenderBinding
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private val binding by viewBinding(FragmentCalenderBinding::bind)
    private val viewModel by viewModels<CalenderViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}