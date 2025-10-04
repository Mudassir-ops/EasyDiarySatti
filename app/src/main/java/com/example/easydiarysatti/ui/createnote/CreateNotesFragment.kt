package com.example.easydiarysatti.ui.createnote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.addTags
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentCreateNotesBinding
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateNotesFragment : Fragment(R.layout.fragment_create_notes) {
    private val binding by viewBinding(FragmentCreateNotesBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private var createNoteEntity: CreateNoteEntity? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNoteEntity = CreateNoteEntity()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlexBox()
        observeNote()
        observeNoteAction()
        clickListeners()
        viewModel.observeNote()
    }

    fun setupFlexBox() {
        binding?.flexboxLayout?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            addTags(mutableListOf("Unknown", "Satti"), onTagClick = {
            }, onRemoveTagClick = { tag ->
            })
        }
    }

    @OptIn(FlowPreview::class)
    fun clickListeners() {
        binding?.apply {

        }
    }


    fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { note ->
                note?.let {
                    binding?.apply {
                        etHeader.setText(it.title)
                        etDescription.setText(it.description)
                    }
                } ?: run {
                    Log.e("observeNote", "observeNote:Null Note ")
                }
            }
        }
    }

    fun observeNoteAction() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notesActionState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { note ->
                    when (note) {
                        CreateNotesState.SaveNote -> {
                            createNoteEntity = CreateNoteEntity(
                                title = binding?.etHeader?.text?.toString().orEmpty(),
                                description = binding?.etHeader?.text?.toString().orEmpty(),
                            )
                            createNoteEntity?.let { viewModel.mergeAndSave(createNoteEntity = it) }
                            findNavController().navigateUp()
                        }

                        CreateNotesState.DiscardNote -> {

                        }

                        is CreateNotesState.ShowMessage -> {

                        }

                        CreateNotesState.BackAction -> findNavController().navigateUp()
                    }
                }
        }
    }

}