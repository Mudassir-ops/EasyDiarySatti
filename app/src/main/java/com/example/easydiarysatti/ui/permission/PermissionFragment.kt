package com.example.easydiarysatti.ui.permission

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLanguageBinding
import com.example.easydiarysatti.databinding.FragmentLanguageBinding.bind
import com.example.easydiarysatti.databinding.FragmentPermissionBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setSelectedBg
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val binding by viewBinding(FragmentPermissionBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
    }

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener { moveToNextScreen() }
        }
    }

    fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.permissionFragment,
            actionId = R.id.action_permissionFragment_to_signUpFragment
        )
    }
}