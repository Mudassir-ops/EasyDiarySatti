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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddTagsFragment : Fragment(R.layout.fragment_add_tags) {

    private val binding by viewBinding(FragmentAddTagsBinding::bind)

    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val nativeViewModel: ViewModelNative by viewModels()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    // ── Mode ───────────────────────────────────────────────────────────────────
    //
    //  isFromDrawer = true  → Drawer "Edit Tags" (outer nav / manage mode)
    //    ┌ drawerToolbar VISIBLE  — fragment owns its own themed header
    //    ├ chip row GONE          — no selection concept
    //    ├ Save Tags GONE         — edits are immediate
    //    ├ Create button VISIBLE  — adds tag to global list (shown immediately)
    //    └ chip tap              → rename dialog
    //
    //  isFromDrawer = false → Create Note "Add Tags" (inner nav / select mode)
    //    ┌ drawerToolbar GONE     — MainFragment toolbar handles the header
    //    ├ chip row VISIBLE when ≥1 tag is selected
    //    ├ Save Tags VISIBLE      — commits selected tags to the note
    //    ├ Create button VISIBLE  — attaches new tag to the current note AND
    //    │                          shows it in All Tags grid immediately
    //    └ chip tap              → select / deselect
    //
    private val isFromDrawer: Boolean by lazy {
        arguments?.getBoolean(FROM_SCREEN, false) ?: false
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private var allNotes: List<CreateNoteEntity>? = null
    private var allUniqueTags: List<CustomTagEntity> = emptyList()
    private val selectedTags: MutableSet<String> = mutableSetOf()

    // ── Adapter ────────────────────────────────────────────────────────────────

    private val tagsAdapter: TagsAdapter by lazy {
        TagsAdapter(
            isManageMode = isFromDrawer,

            onSelectTag = { tagName ->
                // SELECT MODE: toggle — if already selected, deselect it
                if (selectedTags.contains(tagName)) {
                    selectedTags.remove(tagName)
                    removeSelectedChip(tagName)
                } else {
                    selectedTags.add(tagName)
                    addSelectedChip(tagName)
                }
                // Update the full grid highlight in one call (does notifyItemRangeChanged)
                tagsAdapter.updateAddedTags(selectedTags.toSet())
                updateSelectedSection()
            },

            onEditTag = { tag ->
                openEditDialog(tag)
            },

            onDeleteTag = { tagName ->
                deleteTagEverywhere(tagName)
            }
        )
    }

    // ── Edit dialog (rename) ───────────────────────────────────────────────────

    private fun openEditDialog(tag: CustomTagEntity) {
        val oldName = tag.tagName.orEmpty()
        editTagDialog(
            sessionManagerRepo = sessionManagerRepo,
            oldTags            = allUniqueTags,
            selectedTag        = tag,
            onUpdateTag        = { updatedList ->

                val newName = updatedList
                    .firstOrNull { it.tagName != oldName }?.tagName
                    ?: updatedList.firstOrNull()?.tagName
                    ?: return@editTagDialog

                // 1. Persist rename on every note that carries the old name
                allNotes?.forEach { note ->
                    if (note.tags?.any { it.tagName == oldName } == true) {
                        val renamed = note.tags!!.map { t ->
                            if (t.tagName == oldName) t.copy(tagName = newName) else t
                        }
                        viewModel.updateTagsForNote(note.noteId, renamed)
                    }
                }

                // 2. Rename in global tags if it lives there (no-op if note-only)
                homeViewModel.renameGlobalTag(oldName, newName)

                // 3. Sync select-mode chip strip (no-op in manage mode)
                if (selectedTags.remove(oldName)) {
                    selectedTags.add(newName)
                    renameSelectedChip(oldName, newName)
                }

                // 3. Reflect rename in local list immediately (no waiting for DB)
                allUniqueTags = allUniqueTags.map { t ->
                    if (t.tagName == oldName) t.copy(tagName = newName) else t
                }
                tagsAdapter.submitTagList(
                    filteredByCurrentQuery(allUniqueTags),
                    selectedTags.toSet()
                )
            }
        )
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding?.apply {

            rvTags.adapter = tagsAdapter
            rvTags.setHasFixedSize(false)

            // Mode-specific setup
            if (isFromDrawer) setupManageModeUi() else setupSelectModeUi()

            // Live search — same in both modes
            etTags.doAfterTextChanged { text ->
                tagsAdapter.filter(text.toString())
            }

            setupBgTheme()
            observeAllNotes()
        }

        homeViewModel.observeAllNotes()
    }

    // ── MANAGE MODE UI ─────────────────────────────────────────────────────────

    private fun setupManageModeUi() {
        binding?.apply {

            // ── Show own toolbar (MainFragment's toolbar is covered by outer nav) ──
            drawerToolbar.visibility = View.VISIBLE
            applyToolbarTheme(drawerToolbar)
            ivDrawerBack.setOnClickListener { findNavController().navigateUp() }
            ivDrawerSearch.setOnClickListener { toggleSearchMode() }

            // ── Hide select-mode only views ───────────────────────────────────
            tvSelectedTagsLabel.visibility = View.GONE
            chipGroupSelected.visibility   = View.GONE
            btnNext.visibility             = View.GONE

            // ── Create adds the tag directly to the All Tags grid ─────────────
            btnCreate.setOnClickListener { createManageTagFromInput() }
        }
    }

    // ── SELECT MODE UI ─────────────────────────────────────────────────────────

    private fun setupSelectModeUi() {
        binding?.apply {

            drawerToolbar.visibility = View.GONE
            btnNext.visibility = View.VISIBLE
            btnNext.isEnabled = false                // disabled until at least one tag selected
            btnNext.alpha = 0.4f                     // visually dimmed when disabled
            btnNext.setOnClickListener { handleSaveAction() }
            btnCreate.setOnClickListener { createNoteTagFromInput() }

            // Pre-select tags already on the current note and build chip strip once
            viewModel.noteState.value?.tags
                ?.mapNotNull { it.tagName }
                ?.forEach { name ->
                    selectedTags.add(name)
                    addSelectedChip(name)
                }
            updateSelectedSection()
        }
    }

    // ── Search toggle (toolbar search icon / MainFragment search icon) ─────────

    fun toggleSearchMode() {
        binding?.apply {
            val isSearching = etTags.hint == getString(R.string.search_tags)
            if (!isSearching) {
                etTags.hint = getString(R.string.search_tags)
                etTags.requestFocus()
            } else {
                etTags.hint = getString(R.string.add_a_new_tag)
                etTags.text?.clear()
                tagsAdapter.filter("")
            }
        }
    }

    // ── Create tag — MANAGE MODE ───────────────────────────────────────────────
    //
    //  Tags live inside notes in this app's data model. To "create" a standalone
    //  tag we piggyback on the ViewModel's AddTag action with a null note entity.
    //  CreateNotesViewModel.AddTag(name, null) should create/update a placeholder
    //  that makes the tag visible in allNotesState. The DB emission then updates
    //  observeAllNotes → the tag appears in the All Tags grid automatically.
    //
    //  If your ViewModel doesn't support null-note AddTag, swap this call out for
    //  whatever mechanism your repo uses to insert a standalone CustomTagEntity.

    private fun createManageTagFromInput() {
        val entered = binding?.etTags?.text.toString().trim()
        if (entered.isEmpty()) return

        val alreadyExists = allUniqueTags.any {
            it.tagName.orEmpty().equals(entered, ignoreCase = true)
        }
        if (alreadyExists) {
            Toast.makeText(context, getString(R.string.tag_already_exists), Toast.LENGTH_SHORT).show()
            return
        }

        // Room insert → globalTagsState Flow emits → observeAllNotes collects
        // → grid updates automatically. No manual allUniqueTags update needed.
        homeViewModel.saveGlobalTag(entered)
        binding?.etTags?.text?.clear()
    }

    // ── Create tag — SELECT MODE ───────────────────────────────────────────────
    //
    //  Attaches the typed tag to the currently-open note.
    //  The DB emission from AddTag → observeAllNotes → grid shows it in All Tags.
    //  The tag is also added to selectedTags + chip strip immediately.
    //  Tapping "Save Tags" later commits all selectedTags to the note.

    // ── Create tag — SELECT MODE ───────────────────────────────────────────────
    //
    //  STEP 1 of 2:  "Create" button
    //
    //  • Saves the tag to global tags (SharedPrefs) so it appears in All Tags
    //    grid immediately and persists across sessions — but does NOT attach it
    //    to the current note yet.
    //  • Adds to the selectedTags set + chip strip so the user sees it selected.
    //  • The note only gets the tag when the user taps "Save Tags" (step 2).

    private fun createNoteTagFromInput() {
        val entered = binding?.etTags?.text.toString().trim()
        if (entered.isEmpty()) return

        // Room insert → flow emits → grid updates automatically
        homeViewModel.saveGlobalTag(entered)

        // Mark as selected immediately — chip strip + teal highlight
        selectedTags.add(entered)
        addSelectedChip(entered)
        updateSelectedSection()
        tagsAdapter.updateAddedTags(selectedTags.toSet())

        binding?.etTags?.text?.clear()
    }

    // ── Save (SELECT MODE only) ────────────────────────────────────────────────
    //
    //  STEP 2 of 2:  "Save Tags" button
    //
    //  • Attaches every tag in selectedTags to the current note via ViewModel.
    //  • Also picks up anything still typed in the input field.
    //  • Navigates back to CreateNotesFragment where the tags are now visible.

    fun handleSaveAction() {
        val currentNote  = viewModel.noteState.value
        val existingTags = currentNote?.tags
            ?.map { it.tagName.orEmpty().lowercase() }
            ?: emptyList()

        // Attach each selected tag to the note (skip ones already on it)
        selectedTags.forEach { tagName ->
            if (!existingTags.contains(tagName.lowercase())) {
                viewModel.sendAction(CreateNotesState.AddTag(tagName, viewModel.noteState.value))
            }
        }

        // Also attach whatever is still typed but not yet tapped "Create"
        val typed = binding?.etTags?.text.toString().trim()
        if (typed.isNotEmpty() && !existingTags.contains(typed.lowercase())) {
            homeViewModel.saveGlobalTag(typed)          // persist to All Tags
            viewModel.sendAction(CreateNotesState.AddTag(typed, viewModel.noteState.value))
        }

        findNavController().navigateUp()
    }

    // ── Selected-chips strip (SELECT MODE only) ────────────────────────────────

    private fun addSelectedChip(tagName: String) {
        val group = binding?.chipGroupSelected ?: return
        for (i in 0 until group.childCount) {
            if ((group.getChildAt(i) as? Chip)?.text.toString() == tagName) return
        }
        val chip = Chip(requireContext()).apply {
            text               = tagName
            isCloseIconVisible = true
            setChipBackgroundColorResource(android.R.color.transparent)
            chipStrokeWidth    = 2f
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
            if (chip?.text.toString() == tagName) { group.removeView(chip); return }
        }
    }

    private fun renameSelectedChip(oldName: String, newName: String) {
        val group = binding?.chipGroupSelected ?: return
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip
            if (chip?.text.toString() == oldName) {
                chip?.text = newName
                chip?.setOnCloseIconClickListener { deselectTag(newName) }
                return
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
        binding?.chipGroupSelected?.isVisible   = has
        // Enable Save Tags only when at least one tag is selected
        binding?.btnNext?.isEnabled = has
        binding?.btnNext?.alpha     = if (has) 1f else 0.4f
    }

    // ── Delete tag globally (BOTH modes) ──────────────────────────────────────

    private fun deleteTagEverywhere(tagName: String) {
        // 1a. Remove from every note in DB
        allNotes?.forEach { note ->
            if (note.tags?.any { it.tagName == tagName } == true) {
                val trimmed = note.tags!!.filter { it.tagName != tagName }
                viewModel.updateTagsForNote(note.noteId, trimmed)
            }
        }

        // 1b. Remove from global_tags table — Room flow emits → grid updates
        homeViewModel.removeGlobalTag(tagName)

        // 2. Remove from select-mode chip strip (no-op in manage mode)
        selectedTags.remove(tagName)
        removeSelectedChip(tagName)
        updateSelectedSection()
    }

    // ── Observe both sources and merge into All Tags grid ────────────────────
    //
    //  Source 1: allNotesState  — tags derived from notes in the DB
    //  Source 2: globalTagsState — standalone tags saved in SharedPreferences
    //
    //  Both flows are combined so any change to either source automatically
    //  refreshes the grid without any manual UI calls.

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                homeViewModel.allNotesState.flowWithLifecycle(viewLifecycleOwner.lifecycle),
                homeViewModel.globalTagsState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
            ) { notesState, globalTagNames ->

                // Tags from saved notes
                val noteTags: List<CustomTagEntity> =
                    if (notesState is HomeNotesState.Success) {
                        allNotes = notesState.notes
                        notesState.notes
                            ?.flatMap { it.tags ?: emptyList() }
                            ?: emptyList()
                    } else emptyList()

                // Standalone global tags (SharedPrefs) as CustomTagEntity
                val globalTags: List<CustomTagEntity> = globalTagNames.map { name ->
                    CustomTagEntity(tagName = name, noteId = 0)
                }

                // Merge: put globalTags first so newly-created tags appear at
                // the top. Then note tags fill in anything not already present.
                // Deduplicate strictly on lowercase name — noteId differences
                // must NOT create separate entries for the same tag name.
                (globalTags + noteTags)
                    .distinctBy { it.tagName.orEmpty().lowercase().trim() }
                    .filter { it.tagName.orEmpty().isNotBlank() }

            }.collect { mergedFromDb ->
                allUniqueTags = mergedFromDb
                tagsAdapter.submitTagList(
                    filteredByCurrentQuery(allUniqueTags),
                    selectedTags.toSet()
                )
                binding?.tvEmpty?.isVisible = allUniqueTags.isEmpty()
                binding?.rvTags?.isVisible  = allUniqueTags.isNotEmpty()
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun filteredByCurrentQuery(list: List<CustomTagEntity>): List<CustomTagEntity> {
        val q = binding?.etTags?.text.toString()
        return if (q.isBlank()) list
        else list.filter { it.tagName.orEmpty().contains(q.trim(), ignoreCase = true) }
    }

    // ── Background theme — applied to root AND to the drawer toolbar ───────────

    private fun setupBgTheme() {
        val bgResource    = sessionManagerRepo.getBgTheme()
        val finalResource = if (bgResource != 0) bgResource else R.drawable.theme_1

        // Root background (parentView)
        binding?.parentView?.let { v ->
            Glide.with(this)
                .load(finalResource)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(object : CustomViewTarget<View, Drawable>(v) {
                    override fun onResourceReady(r: Drawable, t: Transition<in Drawable>?) { v.background = r }
                    override fun onResourceCleared(p: Drawable?)                           { v.background = null }
                    override fun onLoadFailed(e: Drawable?)                                { v.setBackgroundResource(R.drawable.theme_1) }
                })
        }
    }

    /**
     * Applies the current theme as the background of the drawer toolbar.
     * Called only in manage mode after the toolbar is made visible.
     */
    private fun applyToolbarTheme(toolbar: LinearLayout) {
        val bgResource    = sessionManagerRepo.getBgTheme()
        val finalResource = if (bgResource != 0) bgResource else R.drawable.theme_1

        Glide.with(this)
            .load(finalResource)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
            .into(object : CustomViewTarget<LinearLayout, Drawable>(toolbar) {
                override fun onResourceReady(r: Drawable, t: Transition<in Drawable>?) { toolbar.background = r }
                override fun onResourceCleared(p: Drawable?)                           { toolbar.background = null }
                override fun onLoadFailed(e: Drawable?)                                { toolbar.setBackgroundResource(R.drawable.theme_1) }
            })
    }
}