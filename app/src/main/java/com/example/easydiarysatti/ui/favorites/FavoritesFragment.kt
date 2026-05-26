package com.example.easydiarysatti.ui.favorites

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentFavoritesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoritesFragment : Fragment(R.layout.fragment_favorites) {

    private val binding by viewBinding(FragmentFavoritesBinding::bind)

    // Dedicated ViewModel — only knows about favorites
    private val viewModel by viewModels<FavoritesViewModel>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    // Shared ViewModel — needed to open a note in CreateNotesFragment
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()

    private val favoritesAdapter: FavoritesAdapter by lazy {
        FavoritesAdapter(
            onItemClick = { note ->
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(null)
                createNotesViewModel.setupNoteEntity(note)
                // Signal MainFragment (which owns the header) to open CreateNote in its
                // inner nav. This keeps CreateNotesFragment inside MainFragment's layout
                // so the header (Save, title, back) is always visible.
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("navigate_to_create", true)
                findNavController().navigateUp()
            },
            onFavClick = { note ->
                viewModel.toggleFavorite(note)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.fav_remove),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarTheme()
        setupRecyclerView()
        observeFavorites()

        binding?.apply {
            ivBack.setOnClickListener {
                // Explicitly clear the key so the outer-nav listener's noteWasTapped
                // check correctly identifies this as Case BACK (no note was tapped).
                // Without this, if the user had previously tapped a note but then
                // pressed Back before CreateNote could open, the key would still be
                // present and the listener would skip the Home reset.
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.remove<Boolean>("navigate_to_create")
                findNavController().navigateUp()
            }

            fabAddNote.setOnClickListener {
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(null)
                // Same pattern as onItemClick — pop back to MainFragment and let its
                // inner nav open CreateNote so the header is present.
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("navigate_to_create", true)
                findNavController().navigateUp()
            }
        }
    }

    private fun setupRecyclerView() {
        binding?.rvFavorites?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoritesAdapter
        }
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoritesState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is FavoritesState.Success -> {
                            showList()
                            favoritesAdapter.submitList(state.favorites)
                        }
                        is FavoritesState.Empty -> showEmptyState()
                        is FavoritesState.Error -> showEmptyState()
                        is FavoritesState.Loading -> Unit
                    }
                }
        }
    }

    private fun showEmptyState() {
        binding?.apply {
            emptyStateLayout.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
        }
    }

    private fun showList() {
        binding?.apply {
            emptyStateLayout.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
        }
    }
}