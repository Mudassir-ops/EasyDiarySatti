package com.example.easydiarysatti.ui.edittags

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.data.local.CreateNoteEntity
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
import com.example.easydiarysatti.utills.editTagDialog
import com.example.easydiarysatti.utills.getCurrentThemeColor
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {


    private val binding by viewBinding(FragmentAddTagsBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val nativeViewModel: ViewModelNative by viewModels()
    private var allNotes: List<CreateNoteEntity>? = null
    // ... inside AddTagsFragment.kt
    private val tagsAdapter: TagsAdapter by lazy {
        TagsAdapter(onItemClick = { triple ->
            val action = triple.second
            val selectedTag = triple.third
            val oldTagName = selectedTag.tagName

            when (action) {
                EDIT_ACTION -> {
                    editTagDialog(
                        oldTags = listOf(selectedTag),
                        selectedTag = selectedTag,
                        onUpdateTag = { updatedList ->
                            val newTagName = updatedList.first().tagName

                            // Loop through all notes to update this tag globally
                            allNotes?.forEach { note ->
                                val hasTag = note.tags?.any { it.tagName == oldTagName } ?: false
                                if (hasTag) {
                                    val updatedTags = note.tags?.map {
                                        if (it.tagName == oldTagName) it.copy(tagName = newTagName) else it
                                    } ?: emptyList()

                                    // Send update to ViewModel/Database
                                    viewModel.updateTagsForNote(note.noteId, updatedTags)
                                }
                            }
                            // Clear search field to refresh the view
                            binding?.etTags?.text?.clear()
                        })
                }

                DELETE_ACTION -> {
                    allNotes?.forEach { note ->
                        val hasTag = note.tags?.any { it.tagName == oldTagName } ?: false
                        if (hasTag) {
                            val updatedTags = note.tags?.filter { it.tagName != oldTagName } ?: emptyList()
                            viewModel.updateTagsForNote(note.noteId, updatedTags)
                        }
                    }
                }

                ITEM_CLICK -> {
                    binding?.etTags?.setText(selectedTag.tagName)
                    binding?.etTags?.setSelection(selectedTag.tagName.length)
                }
            }
        })
    }

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {

            (activity as? AppCompatActivity)?.supportActionBar?.hide()
            val themeColor = getCurrentThemeColor(sessionManagerRepo)
//            btnNext.backgroundTintList = ColorStateList.valueOf(themeColor)

            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                handleSaveAction()
            }

            btnNext.setOnClickListener {
                handleSaveAction()
            }

            ivBack.setOnClickListener { findNavController().navigateUp() }

            etTags.doAfterTextChanged {
                // Filter the list as you type
                tagsAdapter.filter(it.toString())
            }

            setupTagRv()
            observeAllNotes()
            setupBgTheme()
//setupNativeAd()
            // UI Setup: Ensure rvTags is always VISIBLE
            val isFromSearch = arguments?.getBoolean(FROM_SCREEN) ?: false
            if (!isFromSearch) {

                btnNext.visibility = View.VISIBLE
                rvTags.visibility = View.VISIBLE
                headerLayout.visibility = View.GONE
                etTagsView.isHintEnabled = true
                etTags.hint = getString(R.string.personal)
            } else {
                btnNext.visibility = View.INVISIBLE
                rvTags.visibility = View.VISIBLE
                headerLayout.visibility = View.VISIBLE
                etTagsView.isHintEnabled = false
                etTags.hint = getString(R.string.search_tags)
            }
        }
        homeViewModel.observeAllNotes()
    }
//    private fun setupNativeAd() {
//        // 1. Observe the LiveData
//        nativeViewModel.adViewLiveData.observe(viewLifecycleOwner) { nativeAd ->
//            if (nativeAd != null) {
//                val adSmallView = AdNativeSmallView(requireContext())
//                binding?.flAdplaceholder?.apply {
//                    removeAllViews()
//                    addView(adSmallView)
//                    adSmallView.setNativeAd(nativeAd)
//                }
//            }
//        }
//
//        // 2. Request the ad (using the ON_BOARDING or appropriate key)
////        nativeViewModel.loadNativeAd(NativeAdKey.EDIT_TAG)
//    }

  fun handleSaveAction() {
        val currentNote = viewModel.noteState.value
        val enteredTag = binding?.etTags?.text.toString().trim()
        val existingTags = currentNote?.tags?.map { it.tagName.lowercase() } ?: emptyList()

        if (enteredTag.isEmpty()) {
            if (currentNote?.tags.isNullOrEmpty()) {
                viewModel.sendAction(CreateNotesState.AddTag("Personal", currentNote))
            }
        } else if (!existingTags.contains(enteredTag.lowercase())) {
            viewModel.sendAction(CreateNotesState.AddTag(enteredTag, currentNote))
        }
        findNavController().navigateUp()
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.allNotesState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    if (state is HomeNotesState.Success) {
                        allNotes = state.notes
                        val uniqueTags = state.notes?.flatMap { it.tags ?: emptyList() }
                            ?.distinctBy { it.tagName.lowercase() }
                            ?: emptyList()

                        // The adapter will now see the name change thanks to the DiffCallback fix
                        tagsAdapter.submitList(uniqueTags)
                    }
                }
        }
    }

    private fun setupTagRv() {
        binding?.rvTags?.apply {
            adapter = tagsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupBgTheme() {
        val bgResource = sessionManagerRepo.getBgTheme()
        val finalResource = if (bgResource != 0) bgResource else R.drawable.theme_1

        binding?.parentView?.let { view ->
            Glide.with(this)
                .load(finalResource)
                // Fix: Force Glide to bypass memory cache to ensure theme change is reflected
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(object : CustomViewTarget<View, Drawable>(view) {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        view.background = resource
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                        view.background = null
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        view.setBackgroundResource(R.drawable.theme_1)
                    }
                })
        }
    }

}