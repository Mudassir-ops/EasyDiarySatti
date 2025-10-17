package com.example.easydiarysatti.ui.edittags

import android.os.Bundle
import android.util.Log
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
import com.example.easydiarysatti.ui.edittags.TagsAdapter.Companion.DELETE_ACTION
import com.example.easydiarysatti.ui.edittags.TagsAdapter.Companion.EDIT_ACTION
import com.example.easydiarysatti.ui.edittags.TagsAdapter.Companion.ITEM_CLICK
import com.example.easydiarysatti.ui.home.HomeNotesState
import com.example.easydiarysatti.ui.home.HomeViewModel
import com.example.easydiarysatti.utills.EditTagDialog
import com.example.easydiarysatti.viewBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {
    private var editDialog: EditTagDialog? = null
    private val binding by viewBinding(FragmentAddTagsBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private var selectedNoteId = -1L
    private val tagsAdapter: TagsAdapter by lazy {
        TagsAdapter(onItemClick = {
            selectedNoteId = it.second.toLong()
            val action = it.second
            when (action) {
                DELETE_ACTION -> {
//                    viewModel.updateTag(
//                        noteId = selectedTagEntity?.noteId ?: -1,
//                        oldTag = selectedTagEntity,
//                        newTag = tag
//                    )
                }

                EDIT_ACTION -> {
                    it.third.let { selectedTag ->
                        editDialog = EditTagDialog(
                            activity = activity ?: return@TagsAdapter,
                            label1 = getString(R.string.edit_tags),
                            label2 = getString(R.string.edit_tags),
                            label3 = getString(R.string.edit),
                            selectedTag = selectedTag,
                            oldTags = it.first.toMutableList(),
                            onUpdateTag = { updatedTags ->

                                Log.e("SattiKhananna", "${Gson().toJson(updatedTags)}: " )
                                // 1️⃣ Update the tag list for that specific note
                                viewModel.updateTagsForNote(
                                    noteId = selectedTag.noteId.toLong(),
                                    newTags = updatedTags
                                )

                                // 2️⃣ Update adapter UI immediately (no need to wait for DB observer)
                                val updatedList = tagsAdapter.currentList.map { tag ->
                                    if (tag.noteId == selectedTag.noteId) {
                                        updatedTags.find { it.tagName == tag.tagName } ?: tag
                                    } else tag
                                }
                                tagsAdapter.submitList(updatedList)
                            },
                            onCancelTag = {
                                editDialog?.dismiss()
                            }
                        )
                        editDialog?.show()
                    }
                }


                ITEM_CLICK -> {

//                        val bundle = Bundle()
//                        bundle.putString("tagName", pair.first.tagName)
//                        bundle.putString(CHECK_NAVIGATION, FROM_TAG_FRAGMENT)
//                        Log.e("itemClick", "onCreate: itemClick send ${pair.first.tagName}")
//
//                        if (findNavController().currentDestination?.id == R.id.tagsFragment) {
//                            findNavController().navigate(
//                                R.id.action_tagsFragment_to_createNotesFragment,
//                                bundle
//                            )
//                        }
                }

            }
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
                        tag = etTags.text.toString(), createNoteEntity = viewModel.noteState.value
                    )
                )
                findNavController().navigateUp()
            }
            btnNext.setOnClickListener {
                if (etTags.text.toString().isEmpty()) {
                    viewModel.sendAction(
                        action = CreateNotesState.AddTag(
                            tag = "Personal", createNoteEntity = viewModel.noteState.value
                        )
                    )
                    findNavController().navigateUp()
                    return@setOnClickListener
                }
                viewModel.sendAction(
                    action = CreateNotesState.AddTag(
                        tag = etTags.text.toString(), createNoteEntity = viewModel.noteState.value
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
            binding?.etTags?.hint = (getString(R.string.personal))
        } else {
            binding?.btnNext?.visibility = View.INVISIBLE
            binding?.headerLayout?.visibility = View.VISIBLE
            binding?.etTagsView?.isHintEnabled = false
            binding?.etTags?.hint = (getString(R.string.search_tags))
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
            homeViewModel.allNotesState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is HomeNotesState.Success -> {
                            val listOfTags =
                                state.notes?.flatMap { it.tags ?: emptyList() } ?: emptyList()
                            Log.e("ListOFTAGSSIZE-->", "observeAllNotes: ${listOfTags.size}")
                            tagsAdapter.submitList(listOfTags)
                        }

                        is HomeNotesState.Error -> {

                        }

                        else -> Unit
                    }
                }
        }
    }


    private fun setupBgTheme() {
        binding?.parentView?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(), placeholder = R.drawable.theme_1
        )
    }
}