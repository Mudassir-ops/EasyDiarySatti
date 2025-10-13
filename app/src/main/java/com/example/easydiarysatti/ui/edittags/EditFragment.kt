package com.example.easydiarysatti.ui.edittags

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentEditBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.viewBinding
import javax.inject.Inject

class EditFragment : Fragment(R.layout.fragment_edit) {
    private var noteEntity: CreateNoteEntity? = null
    private val binding by viewBinding(FragmentEditBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            btnNext.setOnClickListener {
                if (etTags.text.toString().isEmpty()) {
                    findNavController().navigateUp()
                    return@setOnClickListener
                }
//                viewModel.sendAction(
//                    action = CreateNotesState.AddTag(
//                        tag = etTags.text.toString(),
//                        createNoteEntity = viewModel.noteState.value
//                    )
//                )
                findNavController().navigateUp()
            }
            ivMenu.setOnClickListener {
                findNavController().navigateUp()
            }
        }
        setupBgTheme()
    }

    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }
}