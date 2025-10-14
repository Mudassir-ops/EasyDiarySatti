package com.example.easydiarysatti.ui.edittags

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentAddTagsBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {
    private val binding by viewBinding(FragmentAddTagsBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()


    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                viewModel.sendAction(
                    action = CreateNotesState.AddTag(
                        tag = etTags.text.toString(),
                        createNoteEntity = viewModel.noteState.value
                    )
                )
                findNavController().navigateUp()
            }
            btnNext.setOnClickListener {
                if (etTags.text.toString().isEmpty()) {
                    viewModel.sendAction(
                        action = CreateNotesState.AddTag(
                            tag = "Personal",
                            createNoteEntity = viewModel.noteState.value
                        )
                    )
                    findNavController().navigateUp()
                    return@setOnClickListener
                }
                viewModel.sendAction(
                    action = CreateNotesState.AddTag(
                        tag = etTags.text.toString(),
                        createNoteEntity = viewModel.noteState.value
                    )
                )
                findNavController().navigateUp()
            }
        }
    }
}