package com.example.easydiarysatti.ui.edittags

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentAddTagsBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.home.HomeNotesState
import com.example.easydiarysatti.ui.home.HomeViewModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {
    private val binding by viewBinding(FragmentAddTagsBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val tagsAdapter: TagsAdapter by lazy {
        TagsAdapter(onItemClick = {

        })
    }

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                if (arguments?.getBoolean(FROM_SCREEN) == false) {
                    findNavController().navigateUp()
                    return@addCallback
                }
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
            ivBack.setOnClickListener {
                findNavController().navigateUp()
            }

            etTags.doAfterTextChanged {
                if (arguments?.getBoolean(FROM_SCREEN) == false) return@doAfterTextChanged
                tagsAdapter.filter(it.toString())
            }
            setupTagRv()
            observeAllNotes()
            setupBgTheme()

        }

        if (arguments?.getBoolean(FROM_SCREEN) == false) {
            binding?.btnNext?.visibility = View.VISIBLE
            binding?.headerLayout?.visibility = View.GONE
            binding?.etTagsView?.isHintEnabled = true
            // binding?.etTags?.setText(getString(R.string.search_tags))
        } else {
            binding?.btnNext?.visibility = View.INVISIBLE
            binding?.headerLayout?.visibility = View.VISIBLE

            binding?.etTagsView?.isHintEnabled = false
        }

    }

    private fun setupTagRv() {
        binding?.apply {
            rvTags.run {
                adapter = tagsAdapter
                hasFixedSize()
            }
        }
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.allNotesState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is HomeNotesState.Success -> {
                            val listOfTags = state.notes
                                ?.flatMap { it.tags ?: emptyList() }
                                ?.distinctBy { it.noteId }
                                ?: emptyList()
                            tagsAdapter.submitList(listOfTags)
                        }

                        is HomeNotesState.Error -> {

                        }

                        else -> Unit
                    }
                }
        }
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.collect {

            }
        }
    }

    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }
}