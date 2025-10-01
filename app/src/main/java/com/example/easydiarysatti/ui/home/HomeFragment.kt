package com.example.easydiarysatti.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
    }

    private fun clickListener() {
        binding?.apply {
            icAddNotes.setOnClickListener { moveToNextScreen() }
        }
    }

    fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.navigation_home,
            actionId = R.id.action_navigation_home_to_navigation_createNote
        )
    }

}