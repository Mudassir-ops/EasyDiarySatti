package com.example.easydiarysatti.utills

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.DialogConfirmationBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Reusable confirmation bottom-sheet dialog.
 *
 * Usage — delete:
 *   ConfirmationDialog.showDelete(childFragmentManager, count = 1,
 *       onConfirm = { viewModel.deleteNote(note) },
 *       onCancel  = { /* e.g. exitSelectionMode() */ }
 *   )
 *
 * Usage — favorite:
 *   ConfirmationDialog.showFavorite(childFragmentManager, isFav = false,
 *       onConfirm = { viewModel.toggleFavorite(note) }
 *   )
 *
 * Usage — bulk delete:
 *   ConfirmationDialog.showDelete(childFragmentManager, count = 3,
 *       onConfirm = { viewModel.deleteSelected() },
 *       onCancel  = { exitSelectionMode() }   // ← deselect all on cancel
 *   )
 */
class ConfirmationDialog : BottomSheetDialogFragment() {

    private var _binding: DialogConfirmationBinding? = null
    private val binding get() = _binding!!

    private var onConfirm: (() -> Unit)? = null

    /**
     * Called when the user taps Cancel OR dismisses the dialog without confirming.
     * Used by bulk-delete to exit selection mode when the user backs out.
     */
    private var onCancel: (() -> Unit)? = null

    // Track whether the user confirmed — so we don't fire onCancel after a confirm dismiss
    private var confirmed = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type  = arguments?.getString(ARG_TYPE) ?: TYPE_DELETE
        val count = arguments?.getInt(ARG_COUNT, 1) ?: 1
        val isFav = arguments?.getBoolean(ARG_IS_FAV, false) ?: false

        when (type) {
            TYPE_DELETE -> {
                binding.ivIcon.setImageResource(R.drawable.ic_delete_x)
                binding.ivIconBg.setBackgroundResource(R.drawable.bg_circle_red_light)
                binding.tvTitle.text = getString(R.string.dialog_delete_title)
                binding.tvSubtitle.text = resources.getQuantityString(
                    R.plurals.dialog_delete_subtitle, count, count
                )
                binding.btnConfirm.text = getString(R.string.delete)
                binding.btnCancel.text  = getString(R.string.cancel)
            }
            TYPE_FAVORITE -> {
                if (isFav) {
                    binding.ivIcon.setImageResource(R.drawable.ic_heart_filled)
                    binding.ivIconBg.setBackgroundResource(R.drawable.bg_circle_red_light)
                    binding.tvTitle.text    = getString(R.string.dialog_unfav_title)
                    binding.tvSubtitle.text = getString(R.string.dialog_unfav_subtitle)
                    binding.btnConfirm.text = getString(R.string.remove)
                } else {
                    binding.ivIcon.setImageResource(R.drawable.ic_heart_filled)
                    binding.tvTitle.text    = getString(R.string.dialog_fav_title)
                    binding.tvSubtitle.text = getString(R.string.dialog_fav_subtitle)
                    binding.btnConfirm.text = getString(R.string.add_to_favorites)
                }
            }
        }

        binding.btnConfirm.setOnClickListener {
            confirmed = true
            onConfirm?.invoke()
            dismiss()
        }

        // Cancel button: dismiss and fire onCancel
        binding.btnCancel.setOnClickListener {
            dismiss()
            // onCancel fires via onDismiss below (confirmed = false)
        }
    }

    /**
     * onDismiss fires for every close path:
     *   • Cancel button tap
     *   • Back-press / swipe-down gesture
     * We only call onCancel if the user did NOT confirm.
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!confirmed) {
            onCancel?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Factory methods ──────────────────────────────────────────────────────

    companion object {
        private const val TAG        = "ConfirmationDialog"
        private const val ARG_TYPE   = "type"
        private const val ARG_COUNT  = "count"
        private const val ARG_IS_FAV = "is_fav"
        const val TYPE_DELETE   = "delete"
        const val TYPE_FAVORITE = "favorite"

        /**
         * Show delete confirmation.
         * @param count     number of notes to be deleted (shown in subtitle)
         * @param onConfirm called when user taps Delete
         * @param onCancel  called when user taps Cancel or dismisses without confirming
         */
        fun showDelete(
            fm: androidx.fragment.app.FragmentManager,
            count: Int = 1,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ): ConfirmationDialog {
            val dialog = ConfirmationDialog().apply {
                this.onConfirm = onConfirm
                this.onCancel  = onCancel
                arguments = Bundle().apply {
                    putString(ARG_TYPE, TYPE_DELETE)
                    putInt(ARG_COUNT, count)
                }
            }
            dialog.show(fm, TAG)
            return dialog
        }

        /**
         * Show favorite/unfavorite confirmation.
         * @param isFav     current favorite state of the note
         * @param onConfirm called when user confirms
         * @param onCancel  called when user cancels (optional)
         */
        fun showFavorite(
            fm: androidx.fragment.app.FragmentManager,
            isFav: Boolean,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ): ConfirmationDialog {
            val dialog = ConfirmationDialog().apply {
                this.onConfirm = onConfirm
                this.onCancel  = onCancel
                arguments = Bundle().apply {
                    putString(ARG_TYPE, TYPE_FAVORITE)
                    putBoolean(ARG_IS_FAV, isFav)
                }
            }
            dialog.show(fm, TAG)
            return dialog
        }
    }
}