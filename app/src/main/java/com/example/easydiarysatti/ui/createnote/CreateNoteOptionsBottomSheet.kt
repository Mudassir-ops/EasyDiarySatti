package com.example.easydiarysatti.utills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.BottomSheetCreateNoteOptionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet for the ⋮ button in the Create Note toolbar.
 * Items: Favorite · Share · Delete
 */
class CreateNoteOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateNoteOptionBinding? = null
    private val binding get() = _binding!!

    var currentNote:      CreateNoteEntity?               = null
    var onFavorite:       ((CreateNoteEntity) -> Unit)?   = null
    var onShare:          (() -> Unit)?                   = null
    var onDelete:         (() -> Unit)?                   = null
    var onFavoriteResult: ((newIsFavorite: Boolean) -> Unit)? = null

    var noteTitle:  String?  = null
    var isFavorite: Boolean  = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateNoteOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvNoteTitle.text = noteTitle.orEmpty()

        // ✅ Reflect correct icon + text when sheet first opens
        refreshFavoriteUI()

        binding.rowFavorite.setOnClickListener {
            val note = currentNote ?: return@setOnClickListener

            // Toggle in DB via ViewModel
            onFavorite?.invoke(note)

            // Flip local state
            val newFavoriteState = !isFavorite
            isFavorite = newFavoriteState

            // ✅ Update icon + text immediately so user sees the change
            refreshFavoriteUI()

            // ✅ Correct toast for each case
            val toastMsg = if (newFavoriteState)
                getString(R.string.add_to_favorites)      // "Added to Favorites"
            else
                getString(R.string.fav_remove)            // "Removed from Favorites"

            Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()

            // Notify caller (MainFragment) so createNoteEntity.isFavorite stays in sync
            onFavoriteResult?.invoke(newFavoriteState)

            dismiss()
        }

        binding.rowShare.setOnClickListener {
            onShare?.invoke()
            dismiss()
        }

        binding.rowDelete.setOnClickListener {
            onDelete?.invoke()
            dismiss()
        }
    }

    /**
     * Updates BOTH the heart icon and the label text to match [isFavorite].
     *
     * Favorited   → filled heart (tinted accent red)  + "Remove from Favorites"
     * Not fav     → outline heart (tinted grey)       + "Add to Favorites"
     */
    private fun refreshFavoriteUI() {
        if (_binding == null) return

        if (isFavorite) {
            // ── Already a favorite ─────────────────────────────────────────
            binding.tvFavoriteLabel.text = getString(R.string.dialog_unfav_title)

            // Filled heart, colored accent
            binding.ivFavoriteIcon.setImageResource(R.drawable.ic_heart_filled)
            binding.ivFavoriteIcon.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.red_color)
        } else {
            // ── Not yet a favorite ─────────────────────────────────────────
            binding.tvFavoriteLabel.text = getString(R.string.favorite)

            // Outline / unfilled heart, grey
            binding.ivFavoriteIcon.setImageResource(R.drawable.ic_heart_outline)
            binding.ivFavoriteIcon.imageTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.grey)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CreateNoteOptionsBottomSheet"

        fun show(
            fm: androidx.fragment.app.FragmentManager,
            note: CreateNoteEntity?,
            onFavorite: (CreateNoteEntity) -> Unit,
            onShare: () -> Unit,
            onDelete: () -> Unit,
            onFavoriteResult: ((newIsFavorite: Boolean) -> Unit)? = null
        ) {
            if (fm.findFragmentByTag(TAG) != null) return

            CreateNoteOptionsBottomSheet().apply {
                this.currentNote      = note
                this.noteTitle        = note?.title
                this.isFavorite       = note?.isFavorite ?: false
                this.onFavorite       = onFavorite
                this.onShare          = onShare
                this.onDelete         = onDelete
                this.onFavoriteResult = onFavoriteResult
            }.show(fm, TAG)
        }
    }
}