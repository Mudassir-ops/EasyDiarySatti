package com.example.easydiarysatti.utills

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.example.easydiarysatti.R
import com.google.android.material.snackbar.Snackbar

object DeleteSuccessToast {

    fun show(anchorView: View, count: Int = 0) {
        val context = anchorView.context

        val snackbar = Snackbar.make(anchorView, "", Snackbar.LENGTH_SHORT)

        // Use snackbar.view directly — casting to SnackbarLayout fails on newer Material versions
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        snackbarView.setPadding(0, 0, 0, 0)

        val customView = LayoutInflater.from(context)
            .inflate(R.layout.layout_delete_success_toast, snackbarView as? FrameLayout, false)

        val message = when {
            count <= 0 -> "Notes successfully deleted"
            count == 1 -> "1 note successfully deleted"
            else       -> "$count notes successfully deleted"
        }
        customView.findViewById<TextView>(R.id.tvSuccessMessage)?.text = message

        (snackbarView as? FrameLayout)?.addView(customView)

        // Mutate gravity in-place — never reassign layoutParams, that causes a crash
        (snackbarView.layoutParams as? FrameLayout.LayoutParams)?.gravity =
            Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL

        snackbar.show()
    }
}