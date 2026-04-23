package com.example.easydiarysatti.paywalls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
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
    private lateinit var billingManager: BillingManager

    private val configName     = "splash_paywall_config"
    private val keyHighlighted = "splash_home_paywall_screen_highlighted_plan"
    private val keyVariant     = "splash_paywall_layout_variant"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val variant = sharedPref.getAdExtraData(configName, keyVariant)
        return if (variant == "Variant B") {
            inflater.inflate(R.layout.fragment_variant2_paywall, container, false)
        } else {
            inflater.inflate(R.layout.fragment_variant1_paywall, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Drift-scroll only the inner features list — everything else stays fixed
        view.findViewById<NestedScrollView>(R.id.featuresScrollView)?.startDriftScroll()

        FirebaseAnalytics.getInstance(requireContext()).logEvent("splash_paywall_display", null)
        // ── Adapty: log paywall view for analytics (does NOT affect billing) ──
        Adapty.getPaywall("splash_paywall") { result ->
            if (result is com.adapty.utils.AdaptyResult.Success)
                Adapty.logShowPaywall(result.value) { }
        }
        billingManager = BillingManager(requireActivity()) { productId ->
            handleSuccess(productId)
        }

        val variant = sharedPref.getAdExtraData(configName, keyVariant)

        // Set dynamic subscription start date (placeholder until billing resolves trial length)
        setSubscriptionStartDate(view)

        // Start connection; once ready, fetch pricing and then wire up the UI
        billingManager.startConnection(onConnected = {
            if (variant == "Variant B") {
                fetchAndSetupVariantB(view)
            } else {
                fetchAndSetupVariantA(view)
            }
        })
    }

    private fun setSubscriptionStartDate(view: View) {
        updateSubscriptionStartDate(view, trialDays = 3)
    }

    private fun updateSubscriptionStartDate(view: View, trialDays: Int) {
        val tv = view.findViewById<android.widget.TextView>(R.id.tvSubscriptionStart) ?: return
        val cal = java.util.Calendar.getInstance()
        if (trialDays > 0) cal.add(java.util.Calendar.DAY_OF_YEAR, trialDays)
        val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        val dateStr = fmt.format(cal.time)
        tv.text = if (trialDays > 0)
            "Your subscription will start on $dateStr. Cancel anytime."
        else
            "Your subscription starts today. Cancel anytime."
    }

    // ──────────────────────────────────────────────────────────────
    // Variant A — single plan (feature screen)
    // ──────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantA(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val productId = when (highlighted) {
            "weekly"  -> PaywallCatalog.SPLASH_WEEKLY
            "monthly" -> PaywallCatalog.SPLASH_MONTHLY
            else      -> PaywallCatalog.SPLASH_ANNUAL
        }

        billingManager.fetchProductDetails(listOf(productId)) { detailsMap ->
            val details = detailsMap[productId]

            if (details != null) {
                // Main price line  — e.g. "$139.99"
                view.findViewById<TextView>(R.id.tvPrice)?.text =
                    PricingHelper.fullPriceLabel(details)

                // Per-month breakdown for annual  — e.g. "($11.66 / month)"
                val perMonth = PricingHelper.perMonthLabel(details)
                view.findViewById<TextView>(R.id.tvPerMonth)?.apply {
                    text       = if (perMonth.isNotEmpty()) "($perMonth)" else ""
                    visibility = if (perMonth.isNotEmpty()) View.VISIBLE else View.GONE
                }

                // Billing cadence  — e.g. "per year"


                // CTA button label  — e.g. "Start your 3-Days Free Trial"
                val trialDays = PricingHelper.freeTrialDays(details)
                val ctaText = if (trialDays > 0)
                    "Start Your $trialDays-Day Free Trial"
                else
                    "Continue"
                view.findViewById<TextView>(R.id.btnContinue)?.text = ctaText

                // Update subscription start date with real trial length
                updateSubscriptionStartDate(view, trialDays)
            }

            setupVariantAClickListeners(view, productId)
        }
    }

    private fun setupVariantAClickListeners(view: View, productId: String) {
        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            billingManager.launchBillingFlow(productId)
        }
        // Variant A: fragment_variant1_paywall — btnContinue inside CardView 24dp radius
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
    }

    // ──────────────────────────────────────────────────────────────
    // Variant B — dual plan (monthly + annual) with fixed badge
    // ──────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantB(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)

        billingManager.fetchProductDetails(
            listOf(PaywallCatalog.SPLASH_MONTHLY, PaywallCatalog.SPLASH_ANNUAL)
        ) { detailsMap ->

            // ── Monthly card ──────────────────────────────────────────
            detailsMap[PaywallCatalog.SPLASH_MONTHLY]?.let { monthly ->
                // e.g. "$12.99\nBilled Monthly"
                view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text =
                    "${PricingHelper.formattedPrice(monthly)}\nBilled ${PricingHelper.billingPeriod(monthly).replaceFirstChar { it.uppercase() }}"
            }

            // ── Annual card ───────────────────────────────────────────
            detailsMap[PaywallCatalog.SPLASH_ANNUAL]?.let { annual ->
                val trialLabel = PricingHelper.freeTrialLabel(annual)       // "3-day free trial"
                val perMonth   = PricingHelper.perMonthLabel(annual)        // "$0.99/month"
                val price      = PricingHelper.formattedPrice(annual)       // "$11.99"

                // Top line: "Annual\n3-day free trial" (or just "Annual")
                view.findViewById<TextView>(R.id.tvAnnualLabel)?.text =
                    if (trialLabel.isNotEmpty()) "Annual\n$trialLabel" else "Annual"

                // Right side: "$11.99\nJust $0.99/month"
                view.findViewById<TextView>(R.id.tvAnnualPrice)?.text =
                    if (perMonth.isNotEmpty()) "$price\nJust $perMonth" else price
            }

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

        // Badge stays FIXED on highlighted plan
        annualBadge?.visibility  = if (highlighted == "annual")  View.VISIBLE else View.GONE
        monthlyBadge?.visibility = if (highlighted == "monthly") View.VISIBLE else View.GONE

        // Initial selection matches highlighted plan
        applySelection(isAnnual = highlighted == "annual", rbMonthly, rbAnnual)

        rlMonthly?.setOnClickListener { applySelection(false, rbMonthly, rbAnnual) }
        rlAnnual?.setOnClickListener  { applySelection(true,  rbMonthly, rbAnnual) }
        view.findViewById<View>(R.id.tvPrivacyPolicy)?.setOnClickListener {
            activity?.privacyPolicyUrl()
        }
        view.findViewById<View>(R.id.tvTerms)?.setOnClickListener {
            activity?.termsUrl()
        }
        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
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
    }

    // ──────────────────────────────────────────────────────────────
    // Shared helpers
    // ──────────────────────────────────────────────────────────────

    private fun applySelection(isAnnual: Boolean, rbM: RadioButton?, rbA: RadioButton?) {
        rbA?.isChecked = isAnnual
        rbM?.isChecked = !isAnnual
    }

    private fun handleSuccess(productId: String) {
        sharedPref.isAppPurchased = true
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
        findNavController().navigate(R.id.action_splashPaywallFragment_to_mainFragment)
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }
}