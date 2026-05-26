package com.example.easydiarysatti.paywalls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.adapty.Adapty
import com.android.billingclient.api.ProductDetails
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.privacyPolicyUrl
import com.example.easydiarysatti.termsUrl
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainPaywallFragment : Fragment() {

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var internetManager: InternetManager

    private lateinit var billingManager: BillingManager

    private val configName     = "main_paywall_config"
    private val keyHighlighted = "main_paywall_screen_highlighted_plan"
    private val keyPlanType    = "main_paywall_screen_plan_type"

    /** Cached product details populated by [fetchAndSetupMultiPlan]. */
    private var cachedDetailsMap: Map<String, ProductDetails> = emptyMap()

    // Read args once so every function can use them
    private val fromCreateNote   by lazy { arguments?.getBoolean(ARG_FROM_CREATE_NOTE,   false) ?: false }
    private val fromRewardedGate by lazy { arguments?.getBoolean(ARG_FROM_REWARDED_GATE, false) ?: false }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_variant2_paywall, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FirebaseAnalytics.getInstance(requireContext()).logEvent("main_paywall_display", null)
        Adapty.getPaywall("main_paywall") { result ->
            if (result is com.adapty.utils.AdaptyResult.Success)
                Adapty.logShowPaywall(result.value) { }
        }

        billingManager = BillingManager(requireActivity()) { productId -> handleSuccess(productId) }

        view.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            FirebaseAnalytics.getInstance(requireContext()).logEvent("main_paywall_cross_clicked", null)
            closePaywall()
        }
        view.findViewById<View>(R.id.tvPrivacyPolicy)?.setOnClickListener {
            activity?.privacyPolicyUrl()
        }
        view.findViewById<View>(R.id.tvTerms)?.setOnClickListener {
            activity?.termsUrl()
        }

        // ── Hide entire view until prices are ready — prevents empty-text blink ─
        view.alpha = 0f

        // ── Start billing connection ───────────────────────────────────────────
        billingManager.startConnection(
            onConnected = { setupUI(view) },
            onFailed    = { setupUIOffline(view) }
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Smooth reveal helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fades the paywall in from transparent once all price data is ready.
     * Called from every code path that finishes populating the UI
     * (online + offline) so there is never a raw text-swap visible to the user.
     */
    private fun revealPaywall(view: View) {
        view.animate()
            .alpha(1f)
            .setDuration(220)
            .start()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // No-internet / slow-internet bottom sheet  (all cases, built in code)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Programmatic BottomSheetDialog — no custom XML layout required.
     *
     * [isSlowInternet] false → "No Internet Connection"  (offline at open or CTA tap)
     *                  true  → "Connection Timeout"       (billing connect timed out)
     *
     * Retry logic:
     *  • If prices not loaded yet (cachedDetailsMap empty) → re-run full setupUI
     *  • If prices already loaded (user mid-session)       → re-launch billing flow
     */
    private fun showConnectionBottomSheet(view: View, isSlowInternet: Boolean = false) {
        val ctx = context ?: return

        val title   = if (isSlowInternet) "Connection Timeout"       else "No Internet Connection"
        val message = if (isSlowInternet)
            "Could not reach the store. This may be due to a slow connection. Please try again."
        else
            "A network connection is required to complete your subscription. Please check your connection and try again."

        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)

        // ── Build layout entirely in code ─────────────────────────────────────
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity     = android.view.Gravity.CENTER_HORIZONTAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, (32 * resources.displayMetrics.density).toInt())
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // Title
        root.addView(android.widget.TextView(ctx).apply {
            text     = title
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity  = android.view.Gravity.CENTER
            val lp   = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (10 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })

        // Message
        root.addView(android.widget.TextView(ctx).apply {
            text      = message
            textSize  = 14f
            gravity   = android.view.Gravity.CENTER
            setLineSpacing(0f, 1.4f)
            val lp    = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (24 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        })

        // Retry button
        val btnRetry = android.widget.Button(ctx).apply {
            text = "Try Again"
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (52 * resources.displayMetrics.density).toInt()
            )
            lp.bottomMargin = (10 * resources.displayMetrics.density).toInt()
            layoutParams = lp
        }
        root.addView(btnRetry)

        // Dismiss text link
        val btnDismiss = android.widget.TextView(ctx).apply {
            text     = "Dismiss"
            textSize = 14f
            gravity  = android.view.Gravity.CENTER
            val pad  = (8 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        root.addView(btnDismiss)

        bottomSheet.setContentView(root)
        bottomSheet.setCancelable(true)

        // ── Button logic ──────────────────────────────────────────────────────
        btnRetry.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                android.widget.Toast.makeText(
                    ctx, "Still no internet. Please check your connection.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            bottomSheet.dismiss()
            billingManager.startConnection(
                onConnected = {
                    if (cachedDetailsMap.isEmpty()) {
                        setupUI(view)
                    } else {
                        val rbAnnual = view.findViewById<RadioButton>(R.id.rbAnnual)
                        val subId = if (rbAnnual?.isChecked == true) PaywallCatalog.MAIN_ANNUAL else PaywallCatalog.MAIN_MONTHLY
                        billingManager.launchBillingFlow(subId)
                    }
                },
                onFailed = { showConnectionBottomSheet(view, isSlowInternet = true) }
            )
        }

        btnDismiss.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Close / navigate
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Close the paywall and return to the correct screen.
     * Simply pops back — MainFragment.setupOuterNavPaywallListener handles inner-nav restore.
     */
    private fun closePaywall() {
        android.util.Log.d(
            "PaywallNav",
            "closePaywall() fromCreateNote=$fromCreateNote fromRewardedGate=$fromRewardedGate — popping back stack"
        )
        // Defer popBackStack by one frame.
        //
        // WHY: popBackStack() is sometimes called from inside a billing purchase callback
        // (handleSuccess → closePaywall). At that moment the FragmentManager is already
        // mid-transaction processing the billing Activity result, which causes
        // FragmentNavigator.onBackStackChangeStarted to call
        // NavController.prepareForTransition() on an entry that was already popped →
        // "Cannot transition entry that is not in the back stack" fatal crash.
        //
        // Posting to the main Handler queues popBackStack() AFTER the current
        // FragmentManager transaction batch finishes, so NavController's back stack is
        // fully settled when we mutate it.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (!isAdded || activity == null) return@post
            try {
                findNavController().popBackStack()
            } catch (e: Exception) {
                android.util.Log.e("PaywallNav", "closePaywall popBackStack failed: ${e.message}")
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI setup
    // ──────────────────────────────────────────────────────────────────────────

    // ──────────────────────────────────────────────────────────────────────────
    // Offline fallback setup  (billing unreachable — no live prices available)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Populates the UI when Google Play billing is unreachable.
     * Shows placeholder text so the paywall renders fully offline.
     * The CTA click will trigger the no-internet bottom sheet.
     */
    private fun setupUIOffline(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)

        view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text  = "Connect to\nsee pricing"
        view.findViewById<TextView>(R.id.tvAnnualLabel)?.text   = "Annual"
        view.findViewById<TextView>(R.id.tvAnnualPrice)?.text   = "Connect to see pricing"
        view.findViewById<TextView>(R.id.btnContinue)?.text     = "Continue"
        view.findViewById<TextView>(R.id.freeTrailDes)?.apply {
            text       = "Internet required to load pricing. Cancel anytime."
            visibility = View.VISIBLE
        }

        setupMultiPlanClickListeners(view, highlighted)

        // ── Reveal after offline placeholders are set ─────────────────────────
        revealPaywall(view)
    }

    private fun setupUI(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        fetchAndSetupMultiPlan(view, highlighted)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Free trial description helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Populates [R.id.freeTrailDes] with a trial summary line, e.g.:
     *   "3-day free trial. $0.99/month starts on May 12. Cancel anytime."
     *
     * Hidden (GONE) when the product carries no trial so it takes up no space.
     */
    private fun updateFreeTrialDescription(view: View, details: ProductDetails) {
        val tv        = view.findViewById<TextView>(R.id.freeTrailDes) ?: return
        val trialDays = PricingHelper.freeTrialDays(details)

        if (trialDays <= 0) {
            val price  = PricingHelper.formattedPrice(details)
            val period = PricingHelper.billingPeriod(details)
            tv.text       = "Billed $price $period. Cancel anytime."
            tv.visibility = View.VISIBLE
            return
        }

        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, trialDays)
        val dateStr  = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(cal.time)
        val perMonth = PricingHelper.perMonthLabel(details)

        tv.text       = "$trialDays-day free trial, then $perMonth starts on $dateStr. Cancel anytime."
        tv.visibility = View.VISIBLE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Multi-plan selection-UI updater
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called every time the user switches between Monthly and Annual.
     *
     * Updates:
     *  • [R.id.btnContinue] text  → "Start Your X-Day Free Trial" or "Continue"
     *  • [R.id.freeTrailDes]      → trial summary line (shown/hidden per plan)
     *
     * Ensures the CTA and disclaimer always reflect the currently selected plan.
     */
    private fun updateSelectionUI(view: View, isAnnual: Boolean) {
        val monthlyDetails  = cachedDetailsMap[PaywallCatalog.MAIN_MONTHLY]
        val annualDetails   = cachedDetailsMap[PaywallCatalog.MAIN_ANNUAL]
        val selectedDetails = if (isAnnual) annualDetails else monthlyDetails

        // ── Monthly card ──────────────────────────────────────────────────────
        monthlyDetails?.let { monthly ->
            view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text =
                "${PricingHelper.formattedPrice(monthly)}\nBilled ${
                    PricingHelper.billingPeriod(monthly).replaceFirstChar { it.uppercase() }
                }"
        }

        // ── Annual card ───────────────────────────────────────────────────────
        annualDetails?.let { annual ->
            val trialLabel = PricingHelper.freeTrialLabel(annual)
            val perMonth   = PricingHelper.perMonthLabel(annual)
            val price      = PricingHelper.formattedPrice(annual)
            view.findViewById<TextView>(R.id.tvAnnualLabel)?.text =
                if (trialLabel.isNotEmpty()) "Annual\n$trialLabel" else "Annual"
            view.findViewById<TextView>(R.id.tvAnnualPrice)?.text =
                if (perMonth.isNotEmpty()) "$price\nJust $perMonth" else price
        }

        // ── CTA button + disclaimer ───────────────────────────────────────────
        val trialDays = if (selectedDetails != null) PricingHelper.freeTrialDays(selectedDetails) else 0
        view.findViewById<TextView>(R.id.btnContinue)?.text =
            if (trialDays > 0) "Start Your $trialDays-Day Free Trial" else "Continue"

        if (selectedDetails != null) updateFreeTrialDescription(view, selectedDetails)
        else view.findViewById<TextView>(R.id.freeTrailDes)?.visibility = View.GONE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Multi-plan setup
    // ──────────────────────────────────────────────────────────────────────────

    private fun fetchAndSetupMultiPlan(view: View, highlighted: String) {
        billingManager.fetchProductDetails(
            listOf(PaywallCatalog.MAIN_MONTHLY, PaywallCatalog.MAIN_ANNUAL)
        ) { detailsMap ->
            // Cache so click-listener callbacks can reference it
            cachedDetailsMap = detailsMap

            // ── Populate all cards + CTA for the default selection ──────────
            updateSelectionUI(view, isAnnual = highlighted == "annual")

            setupMultiPlanClickListeners(view, highlighted)

            // ── All prices are set — fade the paywall in smoothly ───────────
            // This runs AFTER updateSelectionUI has written every price label,
            // so the user never sees the empty-text state that caused blinking.
            revealPaywall(view)
        }
    }

    private fun setupMultiPlanClickListeners(view: View, highlighted: String) {
        val rlMonthly    = view.findViewById<RelativeLayout>(R.id.rlMonthly)
        val rlAnnual     = view.findViewById<RelativeLayout>(R.id.rlAnnual)
        val rbMonthly    = view.findViewById<RadioButton>(R.id.rbMonthly)
        val rbAnnual     = view.findViewById<RadioButton>(R.id.rbAnnual)
        val monthlyBadge = view.findViewById<TextView>(R.id.monthlyBadge)
        val annualBadge  = view.findViewById<TextView>(R.id.annualBadge)

        annualBadge?.visibility  = if (highlighted == "annual")  View.VISIBLE else View.GONE
        monthlyBadge?.visibility = if (highlighted == "monthly") View.VISIBLE else View.GONE
        applySelection(isAnnual = highlighted == "annual", rbMonthly, rbAnnual)

        val selectMonthly = View.OnClickListener {
            applySelection(false, rbMonthly, rbAnnual)
            updateSelectionUI(view, isAnnual = false)
        }
        val selectAnnual = View.OnClickListener {
            applySelection(true, rbMonthly, rbAnnual)
            updateSelectionUI(view, isAnnual = true)
        }

        rlMonthly?.setOnClickListener(selectMonthly)
        for (i in 0 until (rlMonthly?.childCount ?: 0)) rlMonthly?.getChildAt(i)?.setOnClickListener(selectMonthly)
        rlAnnual?.setOnClickListener(selectAnnual)
        for (i in 0 until (rlAnnual?.childCount ?: 0)) rlAnnual?.getChildAt(i)?.setOnClickListener(selectAnnual)

        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                showConnectionBottomSheet(view)
                return@setOnClickListener
            }
            val subId = if (rbAnnual?.isChecked == true) PaywallCatalog.MAIN_ANNUAL else PaywallCatalog.MAIN_MONTHLY
            billingManager.launchBillingFlow(subId)
        }
        view.findViewById<View>(R.id.btnContinue)?.startShimmerDp(4f)
    }

    private fun applySelection(isAnnual: Boolean, rbM: RadioButton?, rbA: RadioButton?) {
        rbA?.isChecked = isAnnual
        rbM?.isChecked = !isAnnual
    }

    private fun handleSuccess(productId: String) {
        sharedPref.isAppPurchased = true
        val event = when (productId) {
            PaywallCatalog.MAIN_WEEKLY  -> "main_paywall_weekly_purchased"
            PaywallCatalog.MAIN_MONTHLY -> "main_paywall_monthly_purchased"
            PaywallCatalog.MAIN_ANNUAL  -> "main_paywall_annual_purchased"
            else                        -> "main_paywall_purchased"
        }
        FirebaseAnalytics.getInstance(requireContext()).logEvent(event, null)
        closePaywall()
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }

    companion object {
        const val ARG_FROM_CREATE_NOTE   = "from_create_note"
        const val ARG_FROM_REWARDED_GATE = "from_rewarded_gate"

        fun args(
            fromCreateNote: Boolean   = false,
            fromRewardedGate: Boolean = false
        ): Bundle = Bundle().apply {
            putBoolean(ARG_FROM_CREATE_NOTE,   fromCreateNote)
            putBoolean(ARG_FROM_REWARDED_GATE, fromRewardedGate)
        }
    }
}