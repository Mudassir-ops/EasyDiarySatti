package com.example.easydiarysatti.paywalls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.ProductDetails
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.core.widget.NestedScrollView
import com.adapty.Adapty
import com.example.easydiarysatti.privacyPolicyUrl
import com.example.easydiarysatti.termsUrl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashPaywallFragment : Fragment() {

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var internetManager: InternetManager

    private lateinit var billingManager: BillingManager

    private val configName     = "splash_paywall_config"
    private val keyHighlighted = "splash_home_paywall_screen_highlighted_plan"
    private val keyVariant     = "splash_paywall_layout_variant"

    /**
     * Cached product details map populated by [fetchAndSetupVariantB].
     * Used to update the CTA button text and trial description on-the-fly
     * whenever the user switches between Monthly and Annual.
     */
    /** Stable FrameLayout wrapper returned as the fragment root. */
    private lateinit var contentWrapper: FrameLayout

    /** Points to whichever paywall layout is currently visible inside [contentWrapper]. */
    private var activeContentView: View? = null

    private var cachedDetailsMap: Map<String, ProductDetails> = emptyMap()

    // ──────────────────────────────────────────────────────────────────────────
    // Variant resolution
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Resolves which layout variant to actually display.
     *
     * If the remote config requests Variant B (trial-offer UI) BUT the user is a
     * returning subscriber whose subscription has expired (they already consumed
     * their free trial), downgrade to Variant A so we never pitch a trial to
     * someone who has already used it.
     */
    private fun resolveVariant(): String {
        val remoteVariant          = sharedPref.getAdExtraData(configName, keyVariant)
        val isReturningExpiredUser = sharedPref.hasUsedFreeTrial && !sharedPref.isAppPurchased

        return if (isReturningExpiredUser && remoteVariant == "Variant B") {
            android.util.Log.d(
                "SplashPaywall",
                "resolveVariant: returning expired user — forcing Variant A (trial already used)"
            )
            "Variant A"
        } else {
            remoteVariant
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contentWrapper = FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Always inflate Variant B first — safe default for all cases.
        // fetchAndSetupVariantA() will swap in Variant A only when billing
        // confirms a free trial exists; no trial → stays on Variant B.
        activeContentView = inflater.inflate(R.layout.fragment_variant2_paywall, contentWrapper, false)
        contentWrapper.addView(activeContentView)

        // ── Hide until prices are fetched — prevents empty-text blink ──────────
        // revealPaywall() is called at the end of every setup path (online +
        // offline) so the user only ever sees the fully-populated paywall.
        contentWrapper.alpha = 0f

        return contentWrapper
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activeContentView?.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()

        FirebaseAnalytics.getInstance(requireContext()).logEvent("splash_paywall_display", null)
        Adapty.getPaywall("splash_paywall") { result ->
            if (result is com.adapty.utils.AdaptyResult.Success)
                Adapty.logShowPaywall(result.value) { }
        }

        billingManager = BillingManager(requireActivity()) { productId -> handleSuccess(productId) }

        val effectiveVariant = resolveVariant()

        // ── Start billing connection ───────────────────────────────────────────
        // onConnected  → full live price fetch from Google Play
        // onFailed     → billing unreachable (no internet / timeout); populate UI
        //                from cached config so the paywall still renders, and the
        //                CTA will show the no-internet bottom sheet when tapped.
        billingManager.startConnection(
            onConnected = {
                if (effectiveVariant == "Variant B") fetchAndSetupVariantB()
                else                                  fetchAndSetupVariantA()
            },
            onFailed = {
                if (effectiveVariant == "Variant B") setupVariantBOffline()
                else                                  setupVariantAOffline()
            }
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Smooth reveal helper
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Fades the paywall in from transparent once all price data is ready.
     * Called at the end of every setup path so the user never sees a raw
     * text-swap or an empty-price state.
     */
    private fun revealPaywall() {
        contentWrapper.animate()
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
     * [productId] null  → prices not loaded yet; Retry re-runs full setup flow
     *             non-null → user mid-session; Retry launches billing flow directly
     */
    private fun showConnectionBottomSheet(productId: String?, isSlowInternet: Boolean = false) {
        val ctx = context ?: return

        val title   = if (isSlowInternet) "Connection Timeout"       else "No Internet Connection"
        val message = if (isSlowInternet)
            "Could not reach the store. This may be due to a slow connection. Please try again."
        else
            "A network connection is required to complete your subscription. Please check your connection and try again."

        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)

        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity     = android.view.Gravity.CENTER_HORIZONTAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, (32 * resources.displayMetrics.density).toInt())
            setBackgroundColor(android.graphics.Color.WHITE)
        }

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

        btnRetry.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                android.widget.Toast.makeText(
                    ctx, "Still no internet. Please check your connection.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            bottomSheet.dismiss()
            val variant = resolveVariant()
            billingManager.startConnection(
                onConnected = {
                    if (productId != null) {
                        billingManager.launchBillingFlow(productId)
                    } else {
                        if (variant == "Variant B") fetchAndSetupVariantB()
                        else                        fetchAndSetupVariantA()
                    }
                },
                onFailed = { showConnectionBottomSheet(productId, isSlowInternet = true) }
            )
        }

        btnDismiss.setOnClickListener { bottomSheet.dismiss() }

        bottomSheet.show()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Subscription start-date helper
    // ──────────────────────────────────────────────────────────────────────────

    private fun updateSubscriptionStartDate(trialDays: Int) {
        val tv = activeContentView?.findViewById<TextView>(R.id.tvSubscriptionStart) ?: return
        val cal = java.util.Calendar.getInstance()
        if (trialDays > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, trialDays)
        val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        tv.text = if (trialDays > 0)
            "Your subscription will start on ${fmt.format(cal.time)}. Cancel anytime."
        else
            "Your subscription starts today. Cancel anytime."
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
    private fun updateFreeTrialDescription(details: ProductDetails) {
        val tv        = activeContentView?.findViewById<TextView>(R.id.freeTrailDes) ?: return
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
    // Variant B — shared selection-UI updater
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Called every time the user switches between Monthly and Annual in Variant B.
     *
     * Updates:
     *  • [R.id.btnContinue] text  → "Start Your X-Day Free Trial" or "Continue"
     *  • [R.id.freeTrailDes]      → trial summary line (shown/hidden per plan)
     *
     * This ensures both the CTA and the disclaimer always reflect the plan that
     * is actually selected, not just the plan that was pre-selected on load.
     */
    private fun updateVariantBSelectionUI(isAnnual: Boolean) {
        val view       = activeContentView ?: return
        val monthlyDetails = cachedDetailsMap[PaywallCatalog.SPLASH_MONTHLY]
        val annualDetails  = cachedDetailsMap[PaywallCatalog.SPLASH_ANNUAL]
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

        if (selectedDetails != null) updateFreeTrialDescription(selectedDetails)
        else view.findViewById<TextView>(R.id.freeTrailDes)?.visibility = View.GONE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Offline fallback setup  (billing unreachable — no live prices available)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Populates Variant A UI when Google Play billing is unreachable.
     * Shows placeholder price text and a static trial description so the
     * paywall renders fully. The CTA click will trigger the no-internet
     * bottom sheet so the user knows they need a connection to subscribe.
     */
    private fun setupVariantAOffline() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val productId = when (highlighted) {
            "weekly"  -> PaywallCatalog.SPLASH_WEEKLY
            "monthly" -> PaywallCatalog.SPLASH_MONTHLY
            else      -> PaywallCatalog.SPLASH_ANNUAL
        }

        // Swap in Variant A layout
        activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        contentWrapper.removeAllViews()
        val variantAView = layoutInflater.inflate(
            R.layout.fragment_variant1_paywall, contentWrapper, false
        )
        activeContentView = variantAView
        contentWrapper.addView(variantAView)

        // Placeholder text — real values load once internet is restored via Retry
        variantAView.findViewById<TextView>(R.id.tvPrice)?.text = "Connect to see pricing"
        variantAView.findViewById<TextView>(R.id.tvPerMonth)?.visibility = View.GONE
        variantAView.findViewById<TextView>(R.id.btnContinue)?.text = "Continue"

//        variantAView.findViewById<TextView>(R.id.freeTrailDes)?.apply {
//            text       = "Internet required to load pricing. Cancel anytime."
//            visibility = View.VISIBLE
//        }

        updateSubscriptionStartDate(0)
        setupVariantAClickListeners(variantAView, productId)

        // ── Reveal after offline placeholders are set ─────────────────────────
        revealPaywall()
    }

    /**
     * Populates Variant B UI when Google Play billing is unreachable.
     * Shows placeholder price text on both plan cards and a static disclaimer.
     * The CTA click will trigger the no-internet bottom sheet.
     */
    private fun setupVariantBOffline() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val view = activeContentView ?: return

        // Plan card placeholders
        view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text = "Connect to\nsee pricing"
        view.findViewById<TextView>(R.id.tvAnnualLabel)?.text  = "Annual"
        view.findViewById<TextView>(R.id.tvAnnualPrice)?.text  = "Connect to see pricing"

        // CTA + disclaimer
        view.findViewById<TextView>(R.id.btnContinue)?.text = "Continue"
        view.findViewById<TextView>(R.id.freeTrailDes)?.apply {
            text       = "Internet required to load pricing. Cancel anytime."
            visibility = View.VISIBLE
        }

        setupVariantBClickListeners(view, highlighted)

        // ── Reveal after offline placeholders are set ─────────────────────────
        revealPaywall()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Variant A — single plan
    // ──────────────────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantA() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val productId = when (highlighted) {
            "weekly"  -> PaywallCatalog.SPLASH_WEEKLY
            "monthly" -> PaywallCatalog.SPLASH_MONTHLY
            else      -> PaywallCatalog.SPLASH_ANNUAL
        }

        billingManager.fetchProductDetails(listOf(productId)) { detailsMap ->
            val details   = detailsMap[productId]
            val trialDays = if (details != null) PricingHelper.freeTrialDays(details) else 0

            // ── No trial → stay on Variant B, set it up and return ────────────
            if (trialDays == 0) {
                android.util.Log.d(
                    "SplashPaywall",
                    "fetchAndSetupVariantA: no trial for $productId — showing Variant B"
                )
                fetchAndSetupVariantB()
                return@fetchProductDetails
            }

            // ── Trial confirmed → swap Variant B out, inflate Variant A in ────
            activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
            contentWrapper.removeAllViews()

            val variantAView = layoutInflater.inflate(
                R.layout.fragment_variant1_paywall, contentWrapper, false
            )
            activeContentView = variantAView
            contentWrapper.addView(variantAView)

//            variantAView.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()

            if (details != null) {
                variantAView.findViewById<TextView>(R.id.tvPrice)?.text =
                    PricingHelper.fullPriceLabel(details)

                val perMonth = PricingHelper.perMonthLabel(details)
                variantAView.findViewById<TextView>(R.id.tvPerMonth)?.apply {
                    text       = if (perMonth.isNotEmpty()) "($perMonth). Cancel anytime" else ""
                    visibility = if (perMonth.isNotEmpty()) View.VISIBLE else View.GONE
                }

                variantAView.findViewById<TextView>(R.id.btnContinue)?.text =
                    "Start Your $trialDays-Day Free Trial"

                updateSubscriptionStartDate(trialDays)
                updateFreeTrialDescription(details)
            }

            setupVariantAClickListeners(variantAView, productId)
        }
    }

    private fun setupVariantAClickListeners(view: View, productId: String) {
        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                showConnectionBottomSheet(productId = productId)
                return@setOnClickListener
            }
            billingManager.launchBillingFlow(productId)
        }
        view.findViewById<View>(R.id.btnContinue)?.startShimmerDp(24f)
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            FirebaseAnalytics.getInstance(requireContext())
                .logEvent("splash_paywall_cross_clicked", null)
            navigateToMain()
        }
        view.findViewById<View>(R.id.tvPrivacyPolicy)?.setOnClickListener {
            activity?.privacyPolicyUrl()
        }
        view.findViewById<View>(R.id.tvTerms)?.setOnClickListener {
            activity?.termsUrl()
        }

        // ── All prices are written — fade the paywall in smoothly ─────────────
        revealPaywall()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Variant B — dual plan (Monthly + Annual with badge)
    // ──────────────────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantB() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)

        billingManager.fetchProductDetails(
            listOf(PaywallCatalog.SPLASH_MONTHLY, PaywallCatalog.SPLASH_ANNUAL)
        ) { detailsMap ->
            // Cache so click-listener callbacks can reference it without a closure
            cachedDetailsMap = detailsMap

            val view = activeContentView ?: return@fetchProductDetails

            // ── Populate all cards + CTA for the default selection ──────────
            // updateVariantBSelectionUI renders both plan cards, the CTA text,
            // and the disclaimer — so a single call covers the initial load.
            updateVariantBSelectionUI(isAnnual = highlighted == "annual")

            setupVariantBClickListeners(view, highlighted)
        }
    }

    private fun setupVariantBClickListeners(view: View, highlighted: String) {
        val rlMonthly    = view.findViewById<RelativeLayout>(R.id.rlMonthly)
        val rlAnnual     = view.findViewById<RelativeLayout>(R.id.rlAnnual)
        val rbMonthly    = view.findViewById<RadioButton>(R.id.rbMonthly)
        val rbAnnual     = view.findViewById<RadioButton>(R.id.rbAnnual)
        val monthlyBadge = view.findViewById<TextView>(R.id.monthlyBadge)
        val annualBadge  = view.findViewById<TextView>(R.id.annualBadge)

        annualBadge?.visibility  = if (highlighted == "annual")  View.VISIBLE else View.GONE
        monthlyBadge?.visibility = if (highlighted == "monthly") View.VISIBLE else View.GONE

        applySelection(isAnnual = highlighted == "annual", rbMonthly, rbAnnual)

        rlMonthly?.setOnClickListener {
            applySelection(isAnnual = false, rbMonthly, rbAnnual)
            updateVariantBSelectionUI(isAnnual = false)
        }
        rlAnnual?.setOnClickListener {
            applySelection(isAnnual = true, rbMonthly, rbAnnual)
            updateVariantBSelectionUI(isAnnual = true)
        }

        view.findViewById<View>(R.id.tvPrivacyPolicy)?.setOnClickListener {
            activity?.privacyPolicyUrl()
        }
        view.findViewById<View>(R.id.tvTerms)?.setOnClickListener {
            activity?.termsUrl()
        }
        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                val rbAnnual = view.findViewById<RadioButton>(R.id.rbAnnual)
                val subId = if (rbAnnual?.isChecked == true) PaywallCatalog.SPLASH_ANNUAL else PaywallCatalog.SPLASH_MONTHLY
                showConnectionBottomSheet(productId = subId)
                return@setOnClickListener
            }
            val subId = if (rbAnnual?.isChecked == true)
                PaywallCatalog.SPLASH_ANNUAL else PaywallCatalog.SPLASH_MONTHLY
            billingManager.launchBillingFlow(subId)
        }
        view.findViewById<View>(R.id.btnContinue)?.startShimmerDp(4f)
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            FirebaseAnalytics.getInstance(requireContext())
                .logEvent("splash_paywall_cross_clicked", null)
            navigateToMain()
        }

        // ── All prices are written — fade the paywall in smoothly ─────────────
        revealPaywall()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun applySelection(isAnnual: Boolean, rbM: RadioButton?, rbA: RadioButton?) {
        rbA?.isChecked = isAnnual
        rbM?.isChecked = !isAnnual
    }

    private fun handleSuccess(productId: String) {
        sharedPref.isAppPurchased   = true
        sharedPref.hasUsedFreeTrial = true

        val event = when (productId) {
            PaywallCatalog.SPLASH_WEEKLY  -> "splash_paywall_weekly_purchased"
            PaywallCatalog.SPLASH_MONTHLY -> "splash_paywall_monthly_purchased"
            PaywallCatalog.SPLASH_ANNUAL  -> "splash_paywall_annual_purchased"
            else                          -> "splash_paywall_purchased"
        }
        FirebaseAnalytics.getInstance(requireContext()).logEvent(event, null)
        navigateToMain()
    }

    private fun navigateToMain() {
        // Defer navigation by one frame.
        //
        // WHY: navigate() is sometimes called from inside a billing purchase callback
        // (handleSuccess → navigateToMain). At that moment the FragmentManager is
        // already mid-transaction processing the billing Activity result, which causes
        // FragmentNavigator.onBackStackChangeStarted to call
        // NavController.prepareForTransition() on an entry that was already popped →
        // "Cannot transition entry that is not in the back stack" fatal crash.
        //
        // Posting to the main Handler queues the navigate() call AFTER the current
        // FragmentManager transaction batch finishes, so NavController's back stack is
        // fully settled when we mutate it.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            if (!isAdded || activity == null) return@post
            try {
                findNavController().navigate(R.id.action_splashPaywallFragment_to_mainFragment)
            } catch (e: Exception) {
                android.util.Log.e("SplashPaywall", "navigateToMain failed: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }
}