package com.example.easydiarysatti.ads.rewarded

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.DialogRewardedLoadingBinding

/**
 * Full-screen dimmed loading dialog shown while a rewarded video ad is loading.
 *
 * ── Remote Config keys (inside the rewarded ad JSON item) ────────────────────
 *   "loading_dialog_show": true   → show this dialog before the ad
 *   "loading_dialog_time": "2000" → minimum ms to display (default 2000)
 *
 * ── Usage (in MainFragment.loadAndShowReward) ─────────────────────────────────
 *   val dialog = RewardedLoadingDialog.show(childFragmentManager)
 *   // ... start loading the ad ...
 *   // when ad is ready AND min time has elapsed:
 *   dialog.dismissAndRun { showRewardedAd(...) }
 *
 * ── Dot animation ─────────────────────────────────────────────────────────────
 *   Four dots cycle left-to-right (like the screenshot), one active at a time,
 *   each step every 400ms → full cycle = 1.6 s, loops indefinitely.
 */
class RewardedLoadingDialog : DialogFragment() {

    private var _binding: DialogRewardedLoadingBinding? = null
    private val binding get() = _binding!!

    private val dotHandler  = Handler(Looper.getMainLooper())
    private var dotIndex    = 0
    private val dotInterval = 400L   // ms per step

    // Callback to run immediately after the dialog is dismissed
    private var onDismissedCallback: (() -> Unit)? = null

    // Whether the ad is ready to show
    private var adReady    = false
    // Whether the minimum display time has elapsed
    private var timeElapsed = false
    // The minimum ms to show the dialog (from remote config)
    var minDisplayMs: Long = 2000L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also { dialog ->
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes?.dimAmount = 0.5f
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogRewardedLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false   // user cannot dismiss manually
        startDotAnimation()
        scheduleMinTime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dotHandler.removeCallbacksAndMessages(null)
        _binding = null
    }

    // ── Dot animation ──────────────────────────────────────────────────────────

    private val dots: List<View> get() = listOf(
        binding.dot1, binding.dot2, binding.dot3, binding.dot4
    )

    private val activeColor:   Int get() = ContextCompat.getColor(requireContext(), R.color.app_primary_color)
    private val inactiveColor: Int get() = Color.parseColor("#D0D8E0")

    private fun startDotAnimation() {
        val step = object : Runnable {
            override fun run() {
                if (_binding == null) return
                dots.forEachIndexed { i, dot ->
                    val color = if (i == dotIndex) activeColor else inactiveColor
                    (dot.background as? GradientDrawable)?.setColor(color)
                        ?: dot.background?.mutate()?.setTint(color)
                        ?: dot.setBackgroundColor(color)
                }
                dotIndex = (dotIndex + 1) % dots.size
                dotHandler.postDelayed(this, dotInterval)
            }
        }
        dotHandler.post(step)
    }

    // ── Min display time ───────────────────────────────────────────────────────

    private fun scheduleMinTime() {
        dotHandler.postDelayed({
            timeElapsed = true
            maybeFinish()
        }, minDisplayMs)
    }

    // ── Called by MainFragment when the ad finishes loading ───────────────────

    /**
     * Signal that the rewarded ad is ready.
     * If the minimum display time has also elapsed, the dialog is dismissed
     * immediately and [onDismissed] is called.
     * Otherwise, [onDismissed] will be called as soon as the timer fires.
     */
    fun onAdReady(onDismissed: () -> Unit) {
        onDismissedCallback = onDismissed
        adReady = true
        maybeFinish()
    }

    /**
     * Signal that the rewarded ad failed to load or timed out.
     * Dismisses the dialog and calls [onDismissed] immediately regardless of
     * the minimum display time — we don't want to block the user any longer.
     */
    fun onAdFailed(onDismissed: () -> Unit) {
        dotHandler.removeCallbacksAndMessages(null)
        onDismissedCallback = onDismissed
        safelyDismiss()
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private fun maybeFinish() {
        if (adReady && timeElapsed) {
            safelyDismiss()
        }
    }

    private fun safelyDismiss() {
        if (!isAdded) {
            onDismissedCallback?.invoke()
            return
        }
        try {
            dismissAllowingStateLoss()
        } catch (e: Exception) {
            // fragment state already saved — ignore
        }
        onDismissedCallback?.invoke()
    }

    companion object {
        const val TAG = "RewardedLoadingDialog"

        /**
         * Shows the loading dialog and returns the instance so the caller can
         * call [onAdReady] or [onAdFailed] when the ad load completes.
         */
        fun show(
            fm:           androidx.fragment.app.FragmentManager,
            minDisplayMs: Long = 2000L
        ): RewardedLoadingDialog {
            // Prevent duplicates
            (fm.findFragmentByTag(TAG) as? RewardedLoadingDialog)?.let { return it }

            return RewardedLoadingDialog().apply {
                this.minDisplayMs = minDisplayMs
            }.also {
                it.show(fm, TAG)
            }
        }
    }
}