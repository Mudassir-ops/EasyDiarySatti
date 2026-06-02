package com.example.easydiarysatti.paywalls

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils

class ProAccessManager(
    private val activity: FragmentActivity,
    private val sharedPref: SharedPreferenceUtils,
    private val billingManager: BillingManager? = null
) {

    companion object {
        /** One-time IN_APP product ID for Remove Ads purchase */
        const val REMOVE_ADS_PRODUCT_ID = "lifetime_inter_close_in_app_purchase"
        /** Fallback price shown if Google Play is unreachable */
        const val REMOVE_ADS_FALLBACK_PRICE = "$9.99 - Lifetime"
    }

    fun onPremiumIconClicked(fromCreateNote: Boolean = false) {
        if (sharedPref.isAppPurchased) return
        navigateToMainPaywall(fromCreateNote)
    }

    /** Returns true if access is granted, false if paywall was launched. */
    fun checkProAccess(fromCreateNote: Boolean = false): Boolean {
        return if (sharedPref.isAppPurchased) true
        else { navigateToMainPaywall(fromCreateNote); false }
    }

    fun onRemoveAdsClicked(fragmentManager: FragmentManager, fromCreateNote: Boolean = false) {
        if (sharedPref.isAppPurchased) return
        showRemoveAdsDialog(fragmentManager, navigateOnDismiss = false, fromCreateNote = fromCreateNote)
    }

    /**
     * Called from interstitial onAdDismissedFullScreenContent ONLY when
     * sharedPref.shouldShowRemoveAdsPopup() has already returned true in the fragment.
     *
     * ⚠️ DO NOT call shouldShowRemoveAdsPopup() here again — it is stateful.
     *
     * @param fragmentManager  MUST be requireActivity().supportFragmentManager
     */
    fun onInterstitialCrossClicked(
        fragmentManager: FragmentManager,
        onAfterDismiss: (() -> Unit)? = null  // caller owns navigation — fixes wrong-screen bug
    ) {
        if (sharedPref.isAppPurchased) return
        showRemoveAdsDialog(fragmentManager, onAfterDismiss = onAfterDismiss)
    }

    /**
     * THE REAL BUG — why "Remove Ads" stops working after tapping "Unlock Premium":
     *
     * 1. User taps "Unlock Premium" pro card.
     * 2. MainPaywallFragment is pushed onto the back stack.
     * 3. User closes/backs out of paywall → popBackStack() runs.
     * 4. During the back-stack pop animation, FragmentManager sets isStateSaved = true
     *    for ~300–500ms while the animation plays and FM restores state.
     * 5. Meanwhile, the 400ms postDelayed in MainFragment.setupProCardSlider fires.
     *    fetchInAppPrice() (which works fine — BillingManager.ensureConnected() handles
     *    any reconnect internally) delivers the price on the UI thread.
     * 6. We call RemoveAdsDialog.show() — but inside show(), the first thing it does is:
     *
     *      if (fragmentManager.isStateSaved) return   ← SILENT EXIT, no dialog shown
     *
     *    The call is dropped with zero feedback. No retry. User sees nothing.
     *
     * THE FIX:
     * After getting the price back from fetchInAppPrice(), check if FM is in saved state.
     * If yes → post a 400ms retry via Handler. By then the animation is fully done
     * and FM will accept the fragment transaction.
     * If no  → show immediately as normal.
     *
     * NOTE: No billingClient.isReady check needed here. BillingManager.fetchInAppPrice()
     * already calls ensureConnected() internally, which auto-reconnects if needed.
     */
    fun showRemoveAdsDialog(
        fragmentManager: FragmentManager,
        navigateOnDismiss: Boolean = false,
        onAfterDismiss: (() -> Unit)? = null,
        fromCreateNote: Boolean = false
    ) {
        if (sharedPref.isAppPurchased) return

        val variant = sharedPref.getRemoveAdsVariant()

        fun tryShow(displayPrice: String) {
            if (fragmentManager.isDestroyed) return

            val showAction = {
                if (!fragmentManager.isDestroyed && !fragmentManager.isStateSaved) {
                    RemoveAdsDialog.show(
                        fragmentManager = fragmentManager,
                        variant         = variant,
                        price           = displayPrice,
                        onRemoveAds     = {
                            if (billingManager != null) {
                                billingManager.launchInAppBillingFlow(REMOVE_ADS_PRODUCT_ID)
                            } else {
                                navigateToMainPaywall(fromCreateNote)
                            }
                        },
                        onDismissed = when {
                            onAfterDismiss != null -> ({ onAfterDismiss() })
                            navigateOnDismiss -> ({
                                try {
                                    activity.findNavController(R.id.nav_host_container).navigateUp()
                                } catch (e: Exception) { }
                            })
                            else -> null
                        }
                    )
                }
            }

            if (fragmentManager.isStateSaved) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    showAction()
                }, 400L)
            } else {
                showAction()
            }
        }

        if (billingManager != null) {
            billingManager.fetchInAppPrice(REMOVE_ADS_PRODUCT_ID) { fetchedPrice ->
                val displayPrice = if (fetchedPrice.isNotEmpty()) "$fetchedPrice - Lifetime"
                else REMOVE_ADS_FALLBACK_PRICE
                tryShow(displayPrice)
            }
        } else {
            tryShow(REMOVE_ADS_FALLBACK_PRICE)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REWARDED GATE — Save Note
    // ─────────────────────────────────────────────────────────────────────────

    fun checkSaveNoteAccess(
        fragmentManager: FragmentManager,
        onWatchAd: () -> Unit,
        onAllowed: () -> Unit,
        onSubscribe: (() -> Unit)? = null,
        fromCreateNote: Boolean = false
    ) {
        if (sharedPref.isAppPurchased) { onAllowed(); return }

        if (sharedPref.shouldShowRewardedForSave()) {
            RewardedGateDialog.show(
                fragmentManager = fragmentManager,
                onWatchAd       = onWatchAd,
                onSubscribe     = onSubscribe ?: { navigateToMainPaywall(fromCreateNote) },
                title           = "Save More Notes",
                subtitle        = "You've used your free saves. Watch a short video to continue, or subscribe.",
                unlockLinkText  = "unlock this save"
            )
        } else {
            sharedPref.saveNoteUsageCount++
            onAllowed()
        }
    }

    fun onSaveNoteRewardEarned(onAllowed: () -> Unit) {
        // Watching ad unlocks this one save. Increment count so quota tracking stays
        // accurate — badge will still show on the next save attempt (quota still exceeded).
        sharedPref.saveNoteUsageCount++
        onAllowed()
    }

    fun checkMediaNoteAccess(
        fragmentManager: FragmentManager,
        onWatchAd: () -> Unit,
        onAllowed: () -> Unit,
        onSubscribe: (() -> Unit)? = null,
        fromCreateNote: Boolean = false
    ) {
        if (sharedPref.isAppPurchased) { onAllowed(); return }

        if (sharedPref.shouldShowRewardedForMedia()) {
            RewardedGateDialog.show(
                fragmentManager = fragmentManager,
                onWatchAd       = onWatchAd,
                onSubscribe     = onSubscribe ?: { navigateToMainPaywall(fromCreateNote) },
                title           = "Attach More Media",
                subtitle        = "You've used your free media quota. Watch a short video to continue, or subscribe.",
                unlockLinkText  = "unlock media upload"
            )
        } else {
            sharedPref.mediaNoteUsageCount++
            onAllowed()
        }
    }

    fun onMediaNoteRewardEarned(onAllowed: () -> Unit) {
        // Same logic as save: watching ad unlocks this one media attach only.
        // Increment count so quota tracking stays accurate — dialog shows again next time.
        sharedPref.mediaNoteUsageCount++
        onAllowed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Navigation helper
    // ─────────────────────────────────────────────────────────────────────────

    /** Called directly from MainFragment when the rewarded gate's Subscribe Now is tapped. */
    fun navigateToMainPaywallPublic(fromCreateNote: Boolean = false) {
        navigateToMainPaywall(fromCreateNote)
    }

    private fun navigateToMainPaywall(fromCreateNote: Boolean = false) {
        try {
            val bundle = android.os.Bundle().apply {
                putBoolean(MainPaywallFragment.ARG_FROM_CREATE_NOTE, fromCreateNote)
            }
            activity.findNavController(R.id.nav_host_container)
                .navigate(R.id.action_global_mainPaywallFragment, bundle)
        } catch (e: Exception) { }
    }
}