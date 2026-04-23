package com.example.easydiarysatti.utills

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils

// ─────────────────────────────────────────────────────────────────────────────
//  InternetPopupSession  —  in-memory session state (resets on process death)
//
//  TWO user actions produce different outcomes:
//
//  1. DISMISS  (X icon OR hardware back press)
//     → popup is suppressed on ALL screens for the rest of this session.
//
//  2. REMIND ME LATER
//     → popup is suppressed on the CURRENT screen until the user navigates
//       away and returns (consumeRemindLater removes the flag so it fires
//       exactly once per tap, not on every subsequent visit).
//     → also increments the persisted counter via
//       SharedPreferenceUtils.recordInternetPopupRemindLater() so counter-mode
//       (RC value N ≥ 1) tracks progress toward the next RC-gated show.
// ─────────────────────────────────────────────────────────────────────────────
object InternetPopupSession {

    /** True after X / back press — suppresses popup on every screen. */
    @Volatile
    var dismissedForSession: Boolean = false
        private set

    /** Screens where "Remind Me Later" was tapped (keyed by screen ID). */
    private val remindLaterScreens = mutableSetOf<String>()

    /** Call when the user taps X or presses the hardware back button. */
    fun dismissForSession() {
        dismissedForSession = true
        remindLaterScreens.clear()
    }

    /**
     * Call when the user taps "Remind Me Later" for a given [screenId].
     * On the next visit to that screen [consumeRemindLater] will return true
     * exactly once, causing the popup to show regardless of the RC gate.
     */
    fun setRemindLater(screenId: String) {
        remindLaterScreens.add(screenId)
    }

    /**
     * Atomically checks and removes the remind-later flag for [screenId].
     * Returns true (popup should show) if the flag was present; false otherwise.
     */
    fun consumeRemindLater(screenId: String): Boolean =
        remindLaterScreens.remove(screenId)

    /** Resets all session state. Call from Application.onStart() or after logout. */
    fun reset() {
        dismissedForSession = false
        remindLaterScreens.clear()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  InternetConnectivityDialog
//
//  Drop-in replacement for the previous version.  Keeps the same plain Dialog
//  pattern and the same XML (dialog_internet_connectivity.xml), now extended
//  with an ivClose view.
//
//  RC key  : "internet_connectivity_display"  (Number / Long in Firebase)
//  Default : -2
//
//  Mode table
//  ┌───────┬────────────────────────────────────────────────────────────────┐
//  │ Value │ Behaviour                                                      │
//  ├───────┼────────────────────────────────────────────────────────────────┤
//  │  -2   │ Show at most once per 24 hours                                │
//  │  -1   │ Show every time no-internet is detected                       │
//  │   0   │ Disabled — popup never shown                                  │
//  │  N≥1  │ Counter-based: show on first trigger, then again every N      │
//  │       │ "Remind Me Later" taps; counter resets after each show        │
//  └───────┴────────────────────────────────────────────────────────────────┘
//
//  Screen IDs (companion constants):
//    SCREEN_HOME        — use in HomeFragment
//    SCREEN_CREATE_NOTE — use in CreateNotesFragment
//
//  Minimal call site in each Fragment:
//
//      private fun showInternetPopupIfNeeded() {
//          InternetConnectivityDialog.showIfNeeded(
//              context    = requireContext(),
//              sharedPref = sharedPref,
//              screenId   = InternetConnectivityDialog.SCREEN_HOME   // or SCREEN_CREATE_NOTE
//          )
//      }
// ─────────────────────────────────────────────────────────────────────────────
class InternetConnectivityDialog private constructor(
    context: Context,
    private val onDismissForSession: () -> Unit,
    private val onRemindLater: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_internet_connectivity, null)
        setContentView(view)

        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.setGravity(Gravity.CENTER)

        // Enforce a minimum width so the card never shrinks too small
        view.minimumWidth = (context.resources.displayMetrics.widthPixels * 0.82).toInt()

        // Outside-touch and back-press = dismiss for session (same as X icon)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        // ── X / close icon ────────────────────────────────────────────────
        view.findViewById<ImageView>(R.id.ivClose)?.setOnClickListener {
            onDismissForSession()
            dismissSafely()
        }

        // ── Turn On Internet button ───────────────────────────────────────
        view.findViewById<Button>(R.id.btnCheckConnectivity)?.setOnClickListener {
            // Opening WiFi settings counts as acknowledging the popup — dismiss for session
            onDismissForSession()
            dismissSafely()
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        // ── Remind Me Later button ────────────────────────────────────────
        view.findViewById<Button>(R.id.btnRemindLater)?.setOnClickListener {
            onRemindLater()
            dismissSafely()
        }

        // ── Hardware back press = dismiss for session (same as X icon) ────
        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onDismissForSession()
                dismissSafely()
                true
            } else {
                false
            }
        }
    }

    private fun dismissSafely() {
        if (isShowing) dismiss()
    }

    // ── Companion: public API ─────────────────────────────────────────────────

    companion object {

        /** Use as the [screenId] argument in HomeFragment. */
        const val SCREEN_HOME        = "home"

        /** Use as the [screenId] argument in CreateNotesFragment. */
        const val SCREEN_CREATE_NOTE = "create_note"

        /**
         * Single entry point — replaces the old showForReturningUser /
         * showForFirstTimeUser pair.
         *
         * Decision order:
         *   1. Internet is available           → skip (nothing to warn about)
         *   2. Session dismissed (X / back)    → skip (suppressed for session)
         *   3. Remind Me Later pending for this screen
         *                                      → consume flag and show
         *                                        (bypasses RC gate; gate already
         *                                         fired earlier in this flow)
         *   4. RC gate: shouldShowInternetConnectivityPopup()
         *                                      → if false, skip
         *   5. All checks passed               → show dialog
         *
         * @param context       Fragment's requireContext().
         * @param sharedPref    Injected SharedPreferenceUtils instance.
         * @param screenId      One of [SCREEN_HOME] or [SCREEN_CREATE_NOTE].
         * @param isInternetConnected  Live connectivity state from InternetManager.
         */
        fun showIfNeeded(
            context: Context,
            sharedPref: SharedPreferenceUtils,
            screenId: String,
            isInternetConnected: Boolean
        ) {
            // 1. Has internet — nothing to show
            if (isInternetConnected) return

            // 2. Already dismissed for this session via X or back press
            if (InternetPopupSession.dismissedForSession) return

            // ── FTU / RU routing ──────────────────────────────────────────────
            // HomeFragment always loads before CreateNotesFragment for everyone.
            // Without this gate the RC check would fire on Home for FTU users
            // and the popup would never appear on CreateNote at all.
            //
            // Rule:
            //   FTU (isInternetPopupFtuDone == false)
            //     → SCREEN_HOME        : skip silently (wait for CreateNote)
            //     → SCREEN_CREATE_NOTE : show, then mark FTU done
            //
            //   RU  (isInternetPopupFtuDone == true)
            //     → SCREEN_HOME        : show normally
            //     → SCREEN_CREATE_NOTE : skip silently (Home handles it)
            // ─────────────────────────────────────────────────────────────────
            val ftuDone = sharedPref.isInternetPopupFtuDone
            when (screenId) {
                SCREEN_HOME -> {
                    // FTU users: Home must not steal the popup from CreateNote
                    if (!ftuDone) return
                }
                SCREEN_CREATE_NOTE -> {
                    // RU users: CreateNote must not re-show what Home already owns
                    if (ftuDone) return
                }
            }

            // 3. "Remind Me Later" was tapped on this screen — consume and show
            val remindLaterPending = InternetPopupSession.consumeRemindLater(screenId)

            // 4. RC gate (skipped if remind-later is pending)
            if (!remindLaterPending && !sharedPref.shouldShowInternetConnectivityPopup()) return

            // 5. Mark FTU done before showing (survives kill-during-dialog)
            if (!ftuDone) sharedPref.isInternetPopupFtuDone = true

            // 6. Show
            InternetConnectivityDialog(
                context = context,
                onDismissForSession = {
                    // X icon, back press, or "Turn On Internet" → suppress session-wide
                    InternetPopupSession.dismissForSession()
                },
                onRemindLater = {
                    // Increment persisted counter (no-op for -2 / -1 modes)
                    sharedPref.recordInternetPopupRemindLater()
                    // Mark this screen so popup re-appears on the next landing
                    InternetPopupSession.setRemindLater(screenId)
                }
            ).show()
        }

        /**
         * Call from Application.onStart() (ProcessLifecycleOwner) to allow the
         * popup to show again in the next foreground session.
         */
        fun resetSession() {
            InternetPopupSession.reset()
        }
    }
}