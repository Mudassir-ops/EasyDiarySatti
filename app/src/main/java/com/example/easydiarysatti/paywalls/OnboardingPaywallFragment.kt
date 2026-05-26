package com.example.easydiarysatti.paywalls

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
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
import jakarta.inject.Inject

@AndroidEntryPoint
class OnboardingPaywallFragment : Fragment() {

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var internetManager: InternetManager

    private lateinit var billingManager: BillingManager

    private val configName     = "onboarding_paywall_config"
    private val keyVariant     = "onboarding_paywall_layout_variant"
    private val keyHighlighted = "splash_onboarding_paywall_screen_highlighted_plan"

    /**
     * Stable FrameLayout wrapper returned as the fragment root.
     * The actual paywall content is swapped inside it at runtime.
     */
    private lateinit var contentWrapper: FrameLayout

    /**
     * Points to whichever paywall layout is currently visible inside [contentWrapper].
     */
    private var activeContentView: View? = null

    /**
     * Cached product details populated by [fetchAndSetupVariantB].
     * Used to update the CTA button text and trial description on-the-fly
     * whenever the user switches between Monthly and Annual.
     */
    private var cachedDetailsMap: Map<String, ProductDetails> = emptyMap()

    // ──────────────────────────────────────────────────────────────────────────
    // Variant resolution
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Resolves which layout variant to actually display.
     *
     * Returning expired users (trial consumed, not currently subscribed) always
     * see Variant B (the no-trial dual-plan picker) regardless of remote config,
     * so we never pitch a trial to someone who has already used it — and we never
     * inflate Variant A only to swap it out after the billing callback fires.
     */
    private fun resolveVariant(): String {
        val remoteVariant          = sharedPref.getAdExtraData(configName, keyVariant)
        val isReturningExpiredUser = sharedPref.hasUsedFreeTrial && !sharedPref.isAppPurchased
        return if (isReturningExpiredUser) {
            android.util.Log.d(
                "OnboardingPaywall",
                "resolveVariant: returning expired user — forcing Variant B"
            )
            "Variant B"
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

        // Always inflate Variant B first — it's the safe default that works for
        // all cases (no trial, returning user, pure Variant B config).
        // fetchAndSetupVariantA() will swap in Variant A only when billing
        // confirms a free trial exists, with no visible flash since Variant B
        // is already showing stable UI while the billing fetch runs.
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

        activeContentView
            ?.findViewById<NestedScrollView>(R.id.featuresScrollView)
            ?.startDriftScroll()

        FirebaseAnalytics.getInstance(requireContext()).logEvent("onboarding_paywall_display", null)
        Adapty.getPaywall("onboarding_paywall") { result ->
            if (result is com.adapty.utils.AdaptyResult.Success)
                Adapty.logShowPaywall(result.value) { }
        }

        billingManager = BillingManager(requireActivity()) { productId -> handleSuccess(productId) }

        val effectiveVariant = resolveVariant()

        // ── Start billing connection ───────────────────────────────────────────
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
     * [productId] null     → prices not loaded yet; Retry re-runs full setup flow
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
    // Subscription start-date helper (uses activeContentView)
    // ──────────────────────────────────────────────────────────────────────────

    private fun setSubscriptionStartDate() {
        updateSubscriptionStartDate(trialDays = 3)
    }

    private fun updateSubscriptionStartDate(trialDays: Int) {
        val tv = activeContentView
            ?.findViewById<TextView>(R.id.tvSubscriptionStart) ?: return
        val cal = java.util.Calendar.getInstance()
        if (trialDays > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, trialDays)
        val fmt     = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        val dateStr = fmt.format(cal.time)
        tv.text = if (trialDays > 0)
            "Your subscription will start on $dateStr. Cancel anytime."
        else
            "Your subscription starts today. Cancel anytime."
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Free trial description helper (uses activeContentView)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Populates [R.id.freeTrailDes] with a trial summary line, e.g.:
     *   "3-day free trial. $0.99/month starts on May 12. Cancel anytime."
     *
     * Hidden (GONE) when the product carries no trial so it takes up no space.
     * Always reads from [activeContentView] so it works after a layout swap.
     */
    private fun updateFreeTrialDescription(details: ProductDetails) {
        val view = activeContentView ?: return
        val tv   = view.findViewById<TextView>(R.id.freeTrailDes) ?: return
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
     * Always operates on [activeContentView] so it works after a layout swap.
     */
    private fun updateVariantBSelectionUI(isAnnual: Boolean) {
        val view       = activeContentView ?: return
        val monthlyDetails = cachedDetailsMap[PaywallCatalog.ONBOARDING_MONTHLY]
        val annualDetails  = cachedDetailsMap[PaywallCatalog.ONBOARDING_ANNUAL]
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

    private fun setupVariantAOffline() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val productId = when (highlighted) {
            "weekly"  -> PaywallCatalog.ONBOARDING_WEEKLY
            "monthly" -> PaywallCatalog.ONBOARDING_MONTHLY
            else      -> PaywallCatalog.ONBOARDING_ANNUAL
        }

        activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        contentWrapper.removeAllViews()
        val variantAView = layoutInflater.inflate(
            R.layout.fragment_variant1_paywall, contentWrapper, false
        )
        activeContentView = variantAView
        contentWrapper.addView(variantAView)

        variantAView.findViewById<TextView>(R.id.tvPrice)?.text  = "Connect to see pricing"
        variantAView.findViewById<TextView>(R.id.tvPerMonth)?.visibility = View.GONE
        variantAView.findViewById<TextView>(R.id.btnContinue)?.text = "Continue"
//        variantAView.findViewById<TextView>(R.id.freeTrailDes)?.apply {
//            text       = "Internet required to load pricing. Cancel anytime."
//            visibility = View.VISIBLE
//        }

        setupVariantAClickListeners(variantAView, productId)

        // ── Reveal after offline placeholders are set ─────────────────────────
        revealPaywall()
    }

    private fun setupVariantBOffline() {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val view = activeContentView ?: return

        view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text = "Connect to\nsee pricing"
        view.findViewById<TextView>(R.id.tvAnnualLabel)?.text  = "Annual"
        view.findViewById<TextView>(R.id.tvAnnualPrice)?.text  = "Connect to see pricing"
        view.findViewById<TextView>(R.id.btnContinue)?.text    = "Continue"
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
            "weekly"  -> PaywallCatalog.ONBOARDING_WEEKLY
            "monthly" -> PaywallCatalog.ONBOARDING_MONTHLY
            else      -> PaywallCatalog.ONBOARDING_ANNUAL
        }

        billingManager.fetchProductDetails(listOf(productId)) { detailsMap ->
            val details   = detailsMap[productId]
            val trialDays = if (details != null) PricingHelper.freeTrialDays(details) else 0

            // ── No trial → stay on Variant B, set it up and return ────────────
            if (trialDays == 0) {
                android.util.Log.d(
                    "OnboardingPaywall",
                    "fetchAndSetupVariantA: no trial for $productId — showing Variant B"
                )
                fetchAndSetupVariantB()
                return@fetchProductDetails
            }

            // ── Trial confirmed → swap Variant B out, inflate Variant A in ────
            // The user has been looking at a stable Variant B screen the whole
            // time, so this single swap happens invisibly (no double-flash).
            activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
            contentWrapper.removeAllViews()

            val variantAView = layoutInflater.inflate(
                R.layout.fragment_variant1_paywall, contentWrapper, false
            )
            activeContentView = variantAView
            contentWrapper.addView(variantAView)

//            variantAView.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()

            // Populate all text fields for Variant A
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
                .logEvent("onboarding_paywall_cross_clicked", null)
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
            listOf(PaywallCatalog.ONBOARDING_MONTHLY, PaywallCatalog.ONBOARDING_ANNUAL)
        ) { detailsMap ->
            // Cache so click-listener callbacks can reference it
            cachedDetailsMap = detailsMap

            val view = activeContentView ?: return@fetchProductDetails

            // ── Populate all cards + CTA for the default selection ──────────
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

        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            if (!internetManager.isInternetConnected) {
                val rbAnnual = view.findViewById<RadioButton>(R.id.rbAnnual)
                val subId = if (rbAnnual?.isChecked == true) PaywallCatalog.ONBOARDING_ANNUAL else PaywallCatalog.ONBOARDING_MONTHLY
                showConnectionBottomSheet(productId = subId)
                return@setOnClickListener
            }
            val subId = if (rbAnnual?.isChecked == true)
                PaywallCatalog.ONBOARDING_ANNUAL else PaywallCatalog.ONBOARDING_MONTHLY
            billingManager.launchBillingFlow(subId)
        }
        view.findViewById<View>(R.id.btnContinue)?.startShimmerDp(4f)
        view.findViewById<View>(R.id.tvPrivacyPolicy)?.setOnClickListener {
            activity?.privacyPolicyUrl()
        }
        view.findViewById<View>(R.id.tvTerms)?.setOnClickListener {
            activity?.termsUrl()
        }
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener {
            FirebaseAnalytics.getInstance(requireContext())
                .logEvent("onboarding_paywall_cross_clicked", null)
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

        val eventName = when (productId) {
            PaywallCatalog.ONBOARDING_WEEKLY  -> "onboarding_paywall_weekly_purchased"
            PaywallCatalog.ONBOARDING_MONTHLY -> "onboarding_paywall_monthly_purchased"
            PaywallCatalog.ONBOARDING_ANNUAL  -> "onboarding_paywall_annual_purchased"
            else                              -> "onboarding_paywall_purchased"
        }
        FirebaseAnalytics.getInstance(requireContext()).logEvent(eventName, null)
        navigateToMain()
    }

    private fun navigateToMain() {
        sharedPref.isFirstTimeUser = false
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
                findNavController().navigate(R.id.action_onboardingPaywall_to_mainFragment)
            } catch (e: Exception) {
                android.util.Log.e("OnboardingPaywall", "navigateToMain failed: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        activeContentView?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }
}