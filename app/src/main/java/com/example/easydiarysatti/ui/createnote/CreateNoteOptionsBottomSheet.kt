package com.example.easydiarysatti.utills

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.BottomSheetCreateNoteOptionBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet for the ⋮ button in the Create Note toolbar.
 *
 * Items:  Templates · Favorite · Preview · Share · Delete
 */
class CreateNoteOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateNoteOptionBinding? = null
    private val binding get() = _binding!!
    var currentNote: CreateNoteEntity? = null
    // Callbacks set by the factory method
    var onTemplates: (() -> Unit)? = null
    var onFavorite:  ((CreateNoteEntity) -> Unit)? = null
    var onPreview:   (() -> Unit)? = null
    var onShare:     (() -> Unit)? = null
    var onDelete:    (() -> Unit)? = null
    var noteTitle:   String?       = null
    var isFavorite:  Boolean       = false

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

        // Header title
        binding.tvNoteTitle.text = noteTitle.orEmpty()

        // Favorite label flips based on current state
        binding.tvFavoriteLabel.text =
            if (isFavorite) getString(R.string.dialog_unfav_title)
            else getString(R.string.favorite)

        binding.rowFavorite.setOnClickListener {
            currentNote?.let { note -> onFavorite?.invoke(note) }
            dismiss()
        }
        binding.rowShare.setOnClickListener     { onShare?.invoke();     dismiss() }
        binding.rowDelete.setOnClickListener    { onDelete?.invoke();    dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CreateNoteOptionsBottomSheet"
        fun show(
            fm: androidx.fragment.app.FragmentManager,
            note: CreateNoteEntity?, // Pass the whole object here
            onFavorite: (CreateNoteEntity) -> Unit,
            onShare: () -> Unit,
            onDelete: () -> Unit
        ) {
            if (fm.findFragmentByTag(TAG) != null) return

            CreateNoteOptionsBottomSheet().apply {
                this.currentNote = note
                this.noteTitle = note?.title
                this.isFavorite = note?.isFavorite ?: false
                this.onFavorite = onFavorite
                this.onShare = onShare
                this.onDelete = onDelete
            }.show(fm, TAG)
        }
    }
}
