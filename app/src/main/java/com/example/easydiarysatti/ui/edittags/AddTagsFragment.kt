package com.example.easydiarysatti.ui.edittags

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentAddTagsBinding
import com.example.easydiarysatti.databinding.FragmentPermissionBinding
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {
    private val binding by viewBinding(FragmentAddTagsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}