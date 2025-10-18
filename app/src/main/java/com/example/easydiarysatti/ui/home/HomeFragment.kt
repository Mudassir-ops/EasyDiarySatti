package com.example.easydiarysatti.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.monthlyFormatDate
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.createnote.NotesItemAdapter
import com.example.easydiarysatti.ui.main.MainState
import com.example.easydiarysatti.ui.main.MainViewModel
import com.example.easydiarysatti.viewBinding
import com.example.easydiarysatti.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val notesItemAdapter: NotesItemAdapter by lazy {
        NotesItemAdapter(onNoteItemClick = { note ->
            createNotesViewModel.clearTags()
            createNotesViewModel.clearImages()
            createNotesViewModel.setupNoteEntity(createNoteEntity = null)
            createNotesViewModel.setupNoteEntity(createNoteEntity = note)
            moveToNextScreen()
        }, onNoteItemLongClick = {
            findNavController().safeNav(
                currentDestId = R.id.homeFragment,
                actionId = R.id.action_homeFragment_to_previewFragment2,
                bundle = Bundle().apply {
                    putLong(NOTE_ID, it.noteId)
                    putBoolean(FROM_SCREEN, true)
                }
            )
        })
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.setMainState(MainState.HomeScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        setupRecyclerView()
        observeAllNotes()
        setupTodayDate()
        observeSortOrder()
        setStyledDateTime(binding?.tvDate ?: return, R.color.track_color)
    }

    private fun clickListener() {
        binding?.apply {
            ivSorting.setOnClickListener {
                viewModel.updateSortOrder()
            }
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.homeFragment,
            actionId = R.id.action_homeFragment_to_createNotesFragment2
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
                            if (state.notes?.isEmpty() == true) return@collect
                            if (viewModel.currentSortOrder) {
                                binding?.rvNotes?.smoothScrollToPosition(
                                    (state.notes?.size ?: 0) - 1
                                )
                            } else {
                                binding?.rvNotes?.smoothScrollToPosition(0)
                            }
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

    private fun observeSortOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortOrder
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { isAscending ->
                    viewModel.currentSortOrder = isAscending == true
                    binding?.ivSorting?.animate()
                        ?.rotation(if (isAscending == true) 0f else 180f)
                        ?.setDuration(100)
                        ?.start()
                }
        }
    }
}