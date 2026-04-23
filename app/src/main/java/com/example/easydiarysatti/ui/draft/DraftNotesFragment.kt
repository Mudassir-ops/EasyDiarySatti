package com.example.easydiarysatti.ui.draft

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easydiarysatti.NOTE_ENTITY
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentDraftNotesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.main.MainFragment
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DraftNotesFragment : Fragment(R.layout.fragment_draft_notes) {

    private val binding by viewBinding(FragmentDraftNotesBinding::bind)
    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    private val viewModel: DraftNotesViewModel by viewModels()
    private val createNotesViewModel: CreateNotesViewModel by activityViewModels()

    private val adapter: DraftItemAdapter by lazy {
        DraftItemAdapter(
            onEditClick   = { draft -> openDraftForEditing(draft) },
            onDeleteClick = { draft ->
                viewModel.deleteDraft(draft.noteId)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.draft_deleted),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarTheme()
        setupToolbar()
        setupRecyclerView()
        observeUiState()
        setClickListeners()
    }
    private fun setupToolbarTheme() {
        val bgResource = sessionManagerRepo.getBgTheme()
        val finalResource = if (bgResource != 0) bgResource else R.drawable.theme_1

        binding?.toolbar?.let { toolbarView ->
            Glide.with(this)
                .load(finalResource)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(object : CustomViewTarget<LinearLayout, Drawable>(toolbarView) {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        toolbarView.background = resource
                    }
                    override fun onResourceCleared(placeholder: Drawable?) {
                        toolbarView.background = null
                    }
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        toolbarView.setBackgroundResource(R.drawable.theme_1)
                    }
                })
        }
    }
    // ── Toolbar ───────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding?.ivBack?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding?.rvDrafts?.adapter = adapter
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collect { state ->
                    when (state) {

                        DraftUiState.Loading -> {
                            binding?.layoutEmpty?.visibility        = View.GONE
                            binding?.rvDrafts?.visibility           = View.GONE
                            binding?.cardProgress?.visibility       = View.GONE
                            binding?.tvSubtitle?.visibility         = View.GONE

                        }

                        DraftUiState.Empty -> {
                            binding?.tvTitle?.text = getString(R.string.draft)

                            binding?.layoutEmpty?.visibility        = View.VISIBLE
                            binding?.rvDrafts?.visibility           = View.GONE
                            binding?.cardProgress?.visibility       = View.GONE
                            binding?.tvSubtitle?.visibility         = View.GONE

                        }

                        is DraftUiState.Success -> {
                            binding?.tvTitle?.text = getString(R.string.draft_notes)

                            binding?.layoutEmpty?.visibility        = View.GONE
                            binding?.tvSubtitle?.visibility         = View.VISIBLE
                            binding?.cardProgress?.visibility       = View.VISIBLE
                            binding?.rvDrafts?.visibility           = View.VISIBLE


                            val count = state.drafts.size
                            binding?.tvPendingCount?.text =
                                resources.getQuantityString(
                                    R.plurals.pending_thoughts,
                                    count,
                                    count
                                )

                            adapter.submitList(state.drafts)
                        }
                    }
                }
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setClickListeners() {
        binding?.btnCreateNote?.setOnClickListener { openFreshNote() }

    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    /**
     * Opens CreateNotesFragment with a blank note.
     *
     * WHY this two-step approach:
     * ────────────────────────────
     * DraftNotesFragment lives in the OUTER nav. If it navigates directly to
     * createNotesFragment2, that push lands on the OUTER stack — outside homeHost.
     * MainFragment.setupNavControllerListener() only watches homeHost, so
     * handleDestinationChange(createNotesFragment) never fires → no header appears.
     *
     * Solution:
     *   Step 1 — navigateUp() pops DraftNotes from the outer nav → mainFragment resumes.
     *   Step 2 — post{} waits for the fragment transaction to fully settle, then
     *            navigateInnerNavToCreateNote() pushes CreateNote into homeHost.
     *            The inner nav listener fires → handleDestinationChange() runs →
     *            header + save button + bottom toolbar appear exactly as normal. ✅
     */
    private fun openFreshNote() {
        createNotesViewModel.clearTags()
        createNotesViewModel.clearImages()
        createNotesViewModel.setupNoteEntity(null)
        // Tell CreateNotesFragment to navigate back to Drafts (not Home) on exit
        createNotesViewModel.openedFromDraft = true

        // Step 1: pop DraftNotes off the outer nav
        findNavController().navigateUp()

        // Step 2: after the transaction settles, navigate inside homeHost
        Handler(Looper.getMainLooper()).post {
            getMainFragment()?.navigateInnerNavToCreateNote()
        }
    }

    /**
     * Opens an existing draft in CreateNotesFragment for continued editing.
     * Same two-step delegation pattern as openFreshNote().
     *
     * isDraft is kept true on the seeded entity. When the user taps Save,
     * CreateNotesFragment calls mergeAndSave() (not mergeAndSaveAsDraft()),
     * which persists isDraft=false — promoting the draft to a published note.
     */
    private fun openDraftForEditing(draft: CreateNoteEntity) {
        createNotesViewModel.clearTags()
        createNotesViewModel.clearImages()
        if (!draft.tags.isNullOrEmpty())   createNotesViewModel.addTags(draft.tags)
        if (!draft.images.isNullOrEmpty()) createNotesViewModel.addImages(draft.images)
        createNotesViewModel.setupNoteEntity(draft)
        // Tell CreateNotesFragment to navigate back to Drafts (not Home) on exit
        createNotesViewModel.openedFromDraft = true

        val bundle = Bundle().apply { putParcelable(NOTE_ENTITY, draft) }

        // Step 1: pop DraftNotes
        findNavController().navigateUp()

        // Step 2: open draft inside homeHost after transaction settles
        Handler(Looper.getMainLooper()).post {
            getMainFragment()?.navigateInnerNavToCreateNote(draft = bundle)
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Finds the MainFragment instance that owns this fragment's outer nav host.
     * Searches both the parent fragment manager and the activity's fragment manager
     * to handle all nesting configurations.
     */
    private fun getMainFragment(): MainFragment? =
        activity?.supportFragmentManager?.fragments
            ?.flatMap { listOf(it) + it.childFragmentManager.fragments }
            ?.filterIsInstance<MainFragment>()
            ?.firstOrNull()
}