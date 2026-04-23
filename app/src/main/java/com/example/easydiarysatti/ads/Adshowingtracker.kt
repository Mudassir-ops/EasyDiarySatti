package com.example.easydiarysatti.ads

/**
 * Global flag that is true while ANY full-screen ad (interstitial, rewarded,
 * rewarded-interstitial) is actively showing.
 *
 * Set to true as soon as the loading dialog appears (or immediately before
 * show() is called when there is no dialog) — before the ad Activity launches
 * and before ProcessLifecycleOwner.onStop fires.
 *
 * Cleared in every dismiss / fail / delayed callback via wrapListener /
 * wrapRewardedListener so it is always reset even if the ad fails to show.
 *
 * Checked in EasyDiaryApplication.onStart() to suppress app-open ads that
 * would otherwise stack on top of a just-dismissed interstitial or rewarded ad.
 */
object AdShowingTracker {
    @Volatile
    var isAdShowing: Boolean = false

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val clearRunnable = Runnable { isAdShowing = false }

    /**
     * Clears the flag after a delay long enough for ProcessLifecycleOwner.onStart
     * to fire and skip showAdOnResume() before the flag resets.
     *
     * Interstitial: onDismissed → onStart fires within ~200ms → 1000ms is safe.
     * Rewarded: onDismissed → reward callback → onStart fires within ~500ms → 2000ms is safe.
     *
     * Any pending clear is cancelled and rescheduled on each call so back-to-back
     * ads don't clear the flag prematurely.
     */
    fun clearWithDelay() {
        handler.removeCallbacks(clearRunnable)
        handler.postDelayed(clearRunnable, 2000L)
    }
}