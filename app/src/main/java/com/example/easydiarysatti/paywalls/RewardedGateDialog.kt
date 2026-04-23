package com.example.easydiarysatti.paywalls

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.easydiarysatti.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Rewarded Video Gate dialog — shown when free quota (save / media) is exceeded.
 *
 * Layout: dialog_rewarded_premium.xml
 *
 * Has:
 *   ivHero            — diary hero illustration (top overlap)
 *   btnClose          — X top-right inside card
 *   tvTitle           — "Unlock Premium Features"
 *   tvSubtitle        — "You can either watch a video..."
 *   llFeatures        — 5 feature icons (Lock, No Ads, Unlimited Notes, Mood, Themes)
 *   btnSubscribe      — "Subscribe Now" → opens Main Paywall
 *   btnWatchVideo     — "Watch Video" → loads & shows rewarded ad
 *   cvDailyLimit      — bottom info card "Daily limit reached"
 *   tvUnlockLink      — "unlock today's entry" tappable link (same as Watch Video)
 *
 * Usage:
 *   RewardedGateDialog.show(
 *       fragmentManager = childFragmentManager,
 *       onWatchAd       = { loadAndShowRewardedAd() },
 *       onSubscribe     = { navController.navigate(R.id.action_global_mainPaywallFragment) }
 *   )
 */
@AndroidEntryPoint
class RewardedGateDialog : DialogFragment() {

    private var onWatchAd: (() -> Unit)?   = null
    private var onSubscribe: (() -> Unit)? = null

    // Optional overrides — if not set, XML default text is used
    private var customTitle: String?    = null
    private var customSubtitle: String? = null
    private var customLimitText: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_rewarded_premium, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Optional text overrides
        customTitle?.let    { view.findViewById<TextView>(R.id.tvTitle)?.text    = it }
        customSubtitle?.let { view.findViewById<TextView>(R.id.tvSubtitle)?.text = it }
        customLimitText?.let {
            view.findViewById<TextView>(R.id.tvUnlockLink)?.text = it
        }

        // X close button (top-right inside card)
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            dismiss()
        }

        // "Subscribe Now" → Main Paywall
        view.findViewById<View>(R.id.btnSubscribe)?.setOnClickListener {
            dismiss()
            onSubscribe?.invoke()
        }
        view.findViewById<View>(R.id.btnSubscribe)?.startShimmerDp(28f)

        // "Watch Video" button  — shimmer matches btn_gradient_pink_purple rounded corners (28dp)
        view.findViewById<View>(R.id.btnWatchVideo)?.startShimmerDp(28f)

        view.findViewById<View>(R.id.btnWatchVideo)?.setOnClickListener {
            val action = onWatchAd
            dismiss()
            // Delay 300ms after dismiss — gives FragmentManager time to fully commit
            // the dismiss transaction before we try to load/show the rewarded ad.
            // view.post{} is not enough when the background sheet is also dismissing.
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed({ action?.invoke() }, 300)
        }

        // "unlock today's entry" link (same action as Watch Video)
        view.findViewById<View>(R.id.tvUnlockLink)?.setOnClickListener {
            val action = onWatchAd
            dismiss()
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed({ action?.invoke() }, 300)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnSubscribe)?.stopShimmer()
        view?.findViewById<View>(R.id.btnWatchVideo)?.stopShimmer()
        super.onDestroyView()
    }

    companion object {

        /**
         * Show the rewarded gate dialog.
         *
         * @param fragmentManager   childFragmentManager of your Fragment
         * @param onWatchAd         called when user taps "Watch Video" or "unlock today's entry"
         * @param onSubscribe       called when user taps "Subscribe Now"
         * @param title             optional override for tvTitle
         * @param subtitle          optional override for tvSubtitle
         * @param unlockLinkText    optional override for tvUnlockLink (e.g. "unlock this save")
         */
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onWatchAd: () -> Unit,
            onSubscribe: () -> Unit,
            title: String?          = null,
            subtitle: String?       = null,
            unlockLinkText: String? = null
        ) {
            RewardedGateDialog().apply {
                this.onWatchAd       = onWatchAd
                this.onSubscribe     = onSubscribe
                this.customTitle     = title
                this.customSubtitle  = subtitle
                this.customLimitText = unlockLinkText
            }.show(fragmentManager, "RewardedGateDialog")
        }
    }
}