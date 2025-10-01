package com.example.easydiarysatti.ui.createnote

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentCreateNotesBinding
import com.example.easydiarysatti.viewBinding

class CreateNotesFragment : Fragment(R.layout.fragment_create_notes) {
    private val binding by viewBinding(FragmentCreateNotesBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}