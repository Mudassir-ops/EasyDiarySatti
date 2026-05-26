package com.example.easydiarysatti.ui.edittags

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.FragmentAddTagsBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.home.HomeNotesState
import com.example.easydiarysatti.ui.home.HomeViewModel
import com.example.easydiarysatti.utills.TagsAdapter
import com.example.easydiarysatti.utills.editTagDialog
import com.example.easydiarysatti.viewBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private val PREDEFINED_TAGS = listOf(
    "Personal", "Work", "Ideas", "Travel", "Health",
    "Finance", "Family", "Goals", "Shopping", "Quotes"
)

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {

    private val binding by viewBinding(FragmentAddTagsBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val nativeViewModel: ViewModelNative by viewModels()

    @Inject lateinit var sessionManagerRepo: SessionManagerRepo

    private val isFromDrawer: Boolean by lazy { arguments?.getBoolean(FROM_SCREEN, false) ?: false }

    // BUG 1 FIX: allNotes was declared but never assigned, so deleteTagEverywhere
    // always iterated over null. Now assigned in observeAllNotes().
    private var allNotes: List<CreateNoteEntity>? = null
    private var allUniqueTags: List<CustomTagEntity> = emptyList()

    // BUG 4 FIX: Track whether predefined tags have been seeded this session.
    // Previously PREDEFINED_TAGS.forEach { saveGlobalTag(it) } ran unconditionally
    // in setupSelectModeUi() on every open, resurrecting renamed/deleted tags.
    private var predefinedTagsSeeded = false

    private val selectedTags: MutableSet<String> = mutableSetOf()

    private val tagsAdapter: TagsAdapter by lazy {
        TagsAdapter(
            isManageMode = isFromDrawer,
            onSelectTag = { tagName ->
                if (selectedTags.contains(tagName)) {
                    selectedTags.remove(tagName)
                    removeSelectedChip(tagName)
                } else {
                    selectedTags.add(tagName)
                    addSelectedChip(tagName)
                }
                tagsAdapter.updateAddedTags(selectedTags.toSet())
                updateSelectedSection()
            },
            onEditTag = { tag -> openEditDialog(tag) },
            onDeleteTag = { tagName -> deleteTagEverywhere(tagName) }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        arguments?.getParcelable<CreateNoteEntity>("CURRENT_NOTE")?.let { existingNote ->
            viewModel.setupNoteEntity(existingNote)
        }
        binding?.apply {
            rvTags.itemAnimator = null
            rvTags.adapter = tagsAdapter
            if (isFromDrawer) setupManageModeUi() else setupSelectModeUi()
            etTags.doAfterTextChanged { text -> tagsAdapter.filter(text.toString()) }
            setupBgTheme()
            observeAllNotes()
        }
        homeViewModel.observeAllNotes()
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                homeViewModel.allNotesState.flowWithLifecycle(viewLifecycleOwner.lifecycle),
                homeViewModel.globalTagsState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
            ) { notesState, globalNames ->

                // BUG 1 FIX: Capture the notes list here so deleteTagEverywhere can use it.
                // Previously allNotes was never assigned anywhere, so note cleanup on delete
                // was silently skipped for all notes.
                val notes = if (notesState is HomeNotesState.Success) notesState.notes else null

                val noteTags: List<CustomTagEntity> = notes
                    ?.flatMap { it.tags ?: emptyList() } ?: emptyList()

                val globalEntities = globalNames.map { CustomTagEntity(tagName = it, noteId = 0) }

                val merged = (globalEntities + noteTags)
                    .distinctBy { it.tagName?.lowercase()?.trim() }
                    .filter { !it.tagName.isNullOrBlank() }

                Pair(notes, merged)
            }
                .distinctUntilChanged()
                .collect { (notes, mergedList) ->
                    // BUG 1 FIX: assign allNotes so deleteTagEverywhere works
                    allNotes = notes

                    allUniqueTags = mergedList

                    // BUG 4 & 5 FIX: Seed predefined tags only on the very first emission
                    // when no tags exist at all, not on every screen open or setup call.
                    // Previously:
                    //   • setupSelectModeUi() called saveGlobalTag unconditionally every open
                    //   • setupManageModeUi() checked allUniqueTags.isEmpty() which was always
                    //     true at setup time (flow hadn't emitted yet), so tags were always re-added
                    // Now the check runs after the flow emits, against real data.
                    if (!predefinedTagsSeeded && mergedList.isEmpty()) {
                        predefinedTagsSeeded = true
                        PREDEFINED_TAGS.forEach { homeViewModel.saveGlobalTag(it) }
                        return@collect // flow will re-emit once tags are saved
                    }
                    predefinedTagsSeeded = true

                    tagsAdapter.submitTagList(mergedList, selectedTags.toSet())
                    binding?.rvTags?.isVisible = mergedList.isNotEmpty()
                }
        }
    }

    private fun setupSelectModeUi() {
        binding?.apply {
            drawerToolbar.visibility = View.GONE
            btnNext.visibility = View.VISIBLE

            if (selectedTags.isEmpty()) {
                val seedSource: List<CustomTagEntity> = viewModel.allTags()
                    .filter { !it.tagName.isNullOrBlank() }
                    .ifEmpty {
                        viewModel.noteState.value
                            ?.tags
                            ?.filter { !it.tagName.isNullOrBlank() }
                            ?: emptyList()
                    }

                seedSource.forEach { entity ->
                    entity.tagName?.let { name ->
                        if (selectedTags.add(name)) addSelectedChip(name)
                    }
                }
                tagsAdapter.updateAddedTags(selectedTags.toSet())
                updateSelectedSection()
            }

            tagsAdapter.updateAddedTags(selectedTags.toSet())
            updateSelectedSection()

            btnCreate.setOnClickListener {
                createNoteTagFromInput()
                binding?.etTags?.text?.clear()
            }

            btnNext.setOnClickListener { handleSaveAction() }

            // BUG 4 FIX: Removed unconditional PREDEFINED_TAGS seeding from here.
            // It now only happens in observeAllNotes() when the DB is truly empty on first launch.
        }
    }

    fun handleSaveAction() {
        val currentNote = viewModel.noteState.value

        val existingTags = currentNote?.tags
            ?.filter { !it.tagName.isNullOrBlank() }
            ?: emptyList()
        val existingNames = existingTags
            .mapNotNull { it.tagName?.lowercase()?.trim() }
            .toSet()

        val newTagEntities = selectedTags
            .filter { !existingNames.contains(it.lowercase().trim()) }
            .map { CustomTagEntity(
                tagName = it,
                noteId  = currentNote?.noteId?.toInt() ?: 0
            )}

        val typed = binding?.etTags?.text.toString().trim()
        val typedEntity = if (typed.isNotEmpty()
            && !existingNames.contains(typed.lowercase())
            && !selectedTags.any { it.equals(typed, ignoreCase = true) }) {
            homeViewModel.saveGlobalTag(typed)
            listOf(CustomTagEntity(
                tagName = typed,
                noteId  = currentNote?.noteId?.toInt() ?: 0
            ))
        } else emptyList()

        val mergedTags = existingTags + newTagEntities + typedEntity

        viewModel.clearTags()
        viewModel.addTags(mergedTags)
        viewModel.tagsConfirmed = true

        val noteId = currentNote?.noteId ?: 0L
        if (noteId > 0L) {
            viewModel.updateTagsForNote(noteId, mergedTags)
        }

        findNavController().navigateUp()
    }

    private fun addSelectedChip(tagName: String) {
        val group = binding?.chipGroupSelected ?: return
        val chip = Chip(requireContext()).apply {
            text = tagName
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.transparent)
            chipStrokeWidth = 2f
            setChipStrokeColorResource(R.color.app_primary_color)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.app_primary_color))
            setOnCloseIconClickListener { deselectTag(tagName) }
        }
        group.addView(chip)
    }

    private fun removeSelectedChip(tagName: String) {
        val group = binding?.chipGroupSelected ?: return
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip
            if (chip?.text.toString() == tagName) {
                group.removeView(chip)
                break
            }
        }
    }

    private fun deselectTag(tagName: String) {
        selectedTags.remove(tagName)
        removeSelectedChip(tagName)
        tagsAdapter.updateAddedTags(selectedTags.toSet())
        updateSelectedSection()
    }

    private fun updateSelectedSection() {
        val has = selectedTags.isNotEmpty()
        binding?.tvSelectedTagsLabel?.isVisible = has
        binding?.chipGroupSelected?.isVisible = has
        binding?.btnNext?.isEnabled = has
        binding?.btnNext?.alpha = if (has) 1f else 0.4f
    }

    private fun filteredByCurrentQuery(list: List<CustomTagEntity>) =
        binding?.etTags?.text.toString().let { q ->
            if (q.isBlank()) list else list.filter { it.tagName.orEmpty().contains(q.trim(), ignoreCase = true) }
        }

    private fun createNoteTagFromInput() {
        val entered = binding?.etTags?.text.toString().trim()
        if (entered.isEmpty()) return
        homeViewModel.saveGlobalTag(entered)
        if (selectedTags.add(entered)) {
            addSelectedChip(entered)
            updateSelectedSection()
            tagsAdapter.updateAddedTags(selectedTags.toSet())
        }
        binding?.etTags?.text?.clear()
    }

    private fun setupManageModeUi() {
        binding?.apply {
            drawerToolbar.visibility = View.VISIBLE
            applyToolbarTheme(drawerToolbar)
            ivDrawerBack.setOnClickListener {
                findNavController().navigateUp() }
            tvSelectedTagsLabel.visibility = View.GONE
            chipGroupSelected.visibility = View.GONE
            btnNext.visibility = View.GONE
            btnCreate.setOnClickListener {
                val entered = etTags.text.toString().trim()
                if (entered.isNotEmpty()) homeViewModel.saveGlobalTag(entered)
                etTags.text?.clear()
            }

            // BUG 5 FIX: Removed the race-condition guard `if (allUniqueTags.isEmpty())` from
            // here. allUniqueTags is always empty at setup time because the flow hasn't emitted
            // yet, so this guard was never effective. Seeding is now handled exclusively in
            // observeAllNotes() after the flow's first real emission.
        }
    }

    private fun setupBgTheme() {
        val bgResource = sessionManagerRepo.getBgTheme().let { if (it != 0) it else R.drawable.theme_1 }
        binding?.parentView?.let { v ->
            Glide.with(this).load(bgResource).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop().into(object : CustomViewTarget<View, Drawable>(v) {
                override fun onResourceReady(r: Drawable, t: Transition<in Drawable>?) { v.background = r }
                override fun onResourceCleared(p: Drawable?) { v.background = null }
                override fun onLoadFailed(e: Drawable?) { v.setBackgroundResource(R.drawable.theme_1) }
            })
        }
    }

    private fun applyToolbarTheme(toolbar: LinearLayout) {
        val bgResource = sessionManagerRepo.getBgTheme().let { if (it != 0) it else R.drawable.theme_1 }
        Glide.with(this).load(bgResource).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop().into(object : CustomViewTarget<LinearLayout, Drawable>(toolbar) {
            override fun onResourceReady(r: Drawable, t: Transition<in Drawable>?) { toolbar.background = r }
            override fun onResourceCleared(p: Drawable?) { toolbar.background = null }
            override fun onLoadFailed(e: Drawable?) { toolbar.setBackgroundResource(R.drawable.theme_1) }
        })
    }

    private fun deleteTagEverywhere(tagName: String) {
        // BUG 1 FIX: allNotes is now properly populated by observeAllNotes(),
        // so this loop actually runs and cleans up tags from every affected note.
        allNotes?.forEach { note ->
            if (note.tags?.any { it.tagName == tagName } == true) {
                val trimmed = note.tags!!.filter { it.tagName != tagName }
                viewModel.updateTagsForNote(note.noteId, trimmed)
            }
        }
        homeViewModel.removeGlobalTag(tagName)
        selectedTags.remove(tagName)
        removeSelectedChip(tagName)
        updateSelectedSection()
    }

    private fun openEditDialog(tag: CustomTagEntity) {
        val oldName = tag.tagName.orEmpty()

        // Snapshot the current names BEFORE the dialog opens so we can diff afterwards.
        val namesBefore: Set<String> = allUniqueTags.mapNotNull { it.tagName }.toSet()

        editTagDialog(
            sessionManagerRepo = sessionManagerRepo,
            oldTags = allUniqueTags,
            selectedTag = tag,
            onUpdateTag = { updatedList ->

                // BUG 2 FIX: The old code used:
                //   updatedList.firstOrNull { it.tagName != oldName }
                // which matches the very first tag in the list that isn't `oldName`
                // (e.g. "Work") — completely wrong for any predefined tag beyond index 0.
                //
                // Correct approach: diff the list against the pre-dialog snapshot to find
                // whichever name is new (i.e. wasn't present before the rename).
                val newName = updatedList
                    .mapNotNull { it.tagName }
                    .firstOrNull { it !in namesBefore }
                    ?: return@editTagDialog

                homeViewModel.renameGlobalTag(oldName, newName)

                // BUG 3 FIX: Removed findNavController().navigateUp() from here.
                // Previously every rename dismissed the entire Manage Tags screen,
                // forcing the user to re-open it for each subsequent edit.
                // The flow in observeAllNotes() will auto-refresh the list after renaming.
            }
        )
    }
}