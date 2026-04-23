package com.example.easydiarysatti.utills


import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.easydiarysatti.R

/**
 * Centered card-style dialog shown when the user presses back on
 * CreateNotesFragment while there are unsaved changes.
 *
 * Layout: dialog_save_draft.xml  (your existing file — no changes needed there)
 *
 * Usage:
 *   SaveDraftDialog.show(
 *       fragmentManager = childFragmentManager,
 *       onSaveAsDraft   = { /* save draft + exit */ },
 *       onCancel        = { /* dismiss — user stays on screen */ },
 *       onExitAnyway    = { /* discard and exit */ }
 *   )
 */
class SaveDraftDialog : DialogFragment() {

    // ── Callbacks ─────────────────────────────────────────────────────────────
    var onSaveAsDraft: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var onExitAnyway: (() -> Unit)? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Transparent window background so the CardView's rounded corners show cleanly
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            requestFeature(Window.FEATURE_NO_TITLE)
        }
        return inflater.inflate(R.layout.dialog_save_draft, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dismiss on outside tap
        isCancelable = true

        // ── Primary: Save as Draft ────────────────────────────────────────────
        view.findViewById<Button>(R.id.btnSaveAsDraft).setOnClickListener {
            dismissAllowingStateLoss()
            onSaveAsDraft?.invoke()
        }

        // ── Secondary: Cancel ────────────────────────────────────────────────
        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismissAllowingStateLoss()
            onCancel?.invoke()
        }

        // ── Tertiary: Exit Anyway (coral/red — no underline per your design) ──
        view.findViewById<TextView>(R.id.tvExitAnyway).setOnClickListener {
            dismissAllowingStateLoss()
            onExitAnyway?.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make the dialog fill horizontal space (the 32dp margins come from the XML itself)
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        dialog?.window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val windowWidth = (displayMetrics.widthPixels * 0.90).toInt()

        dialog?.window?.setLayout(windowWidth, WindowManager.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setGravity(Gravity.CENTER)

        // Tapping outside the card dismisses without triggering onExit
        setCancelable(true)

    }

    // ── Companion factory ─────────────────────────────────────────────────────

    companion object {
        const val TAG = "SaveDraftDialog"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onSaveAsDraft: () -> Unit,
            onCancel: () -> Unit,
            onExitAnyway: () -> Unit
        ) {
            // Guard against double-showing (e.g. rapid back taps)
            if (fragmentManager.findFragmentByTag(TAG) != null) return

            SaveDraftDialog().apply {
                this.onSaveAsDraft = onSaveAsDraft
                this.onCancel      = onCancel
                this.onExitAnyway  = onExitAnyway
            }.show(fragmentManager, TAG)
        }
    }
}