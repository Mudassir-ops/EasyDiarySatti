package com.example.easydiarysatti.utills

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.easydiarysatti.R

/**
 * Bulk Delete Confirmation Dialog
 *
 * Shown when the user long-presses to select multiple notes and taps Delete.
 * Displays the count of selected notes dynamically.
 *
 * Design: Matches Multi-select___Delete_UI.png
 *   - Red/pink icon circle with X
 *   - "Permanently Delete Notes?"
 *   - "You're about to delete 'N' notes. This action cannot be reversed."
 *   - [Delete]  — filled teal pill
 *   - [Cancel]  — light teal muted pill
 *
 * Usage:
 *   BulkDeleteDialog.show(
 *       context    = requireContext(),
 *       count      = selectedNotes.size,
 *       onDelete   = { viewModel.deleteSelectedNotes(selectedNotes) },
 *       onCancel   = { /* clear selection */ }
 *   )
 */
class BulkDeleteDialog(
    context: Context,
    private val count: Int,
    private val onDelete: () -> Unit,
    private val onCancel: (() -> Unit)? = null
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_bulk_delete_confirm, null)
        setContentView(view)

        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)

        setCancelable(true)
        setCanceledOnTouchOutside(true)

        // Update body text with the actual count
        view.findViewById<TextView>(R.id.tvDeleteBody)?.text =
            "You're about to delete '$count' ${if (count == 1) "note" else "notes"}. This\naction cannot be reversed."

        view.findViewById<Button>(R.id.btnDelete)?.setOnClickListener {
            dismissSafely()
            onDelete()
        }

        view.findViewById<Button>(R.id.btnCancelDelete)?.setOnClickListener {
            dismissSafely()
            onCancel?.invoke()
        }

        setOnCancelListener { onCancel?.invoke() }
    }

    private fun dismissSafely() { if (isShowing) dismiss() }

    companion object {
        fun show(
            context: Context,
            count: Int,
            onDelete: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) = BulkDeleteDialog(context, count, onDelete, onCancel).show()
    }
}