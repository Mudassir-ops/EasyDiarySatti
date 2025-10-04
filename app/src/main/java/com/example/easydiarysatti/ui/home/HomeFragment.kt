package com.example.easydiarysatti.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.monthlyFormatDate
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.createnote.NotesItemAdapter
import com.example.easydiarysatti.viewBinding
import com.example.easydiarysatti.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val notesItemAdapter: NotesItemAdapter by lazy {
        NotesItemAdapter(onNoteItemClick = { note ->
            createNotesViewModel.setCurrentNoteId(noteId = note.noteId)
            moveToNextScreen()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        setupRecyclerView()
        observeAllNotes()
        setupTodayDate()
    }

    private fun clickListener() {
        binding?.apply {
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.homeFragment,
            actionId = R.id.action_homeFragment_to_createNotesFragment
        )
    }

    private fun setupRecyclerView() {
        binding?.rvNotes?.run {
            adapter = notesItemAdapter
            hasFixedSize()
        }
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allNotesState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is HomeNotesState.Success -> {
                            binding?.visible(hasNotes = true)
                            notesItemAdapter.submitList(state.notes)
                        }

                        is HomeNotesState.Error -> {
                            binding?.visible(hasNotes = false)
                        }

                        else -> Unit
                    }
                }
        }
    }

    private fun setupTodayDate() {
        binding?.tvDate?.apply {
            val currentTimestamp = System.currentTimeMillis()
            val formattedDate = context?.monthlyFormatDate(currentTimestamp)
            text = formattedDate
        }
    }

}