package com.example.easydiarysatti.paywalls

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
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
    private lateinit var billingManager: BillingManager

    private val configName       = "onboarding_paywall_config"
    private val keyVariant       = "onboarding_paywall_layout_variant"
    private val keyHighlighted   = "splash_onboarding_paywall_screen_highlighted_plan"

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

        FirebaseAnalytics.getInstance(requireContext()).logEvent("onboarding_paywall_display", null)
        Adapty.getPaywall("onboarding_paywall") { result ->
            if (result is com.adapty.utils.AdaptyResult.Success)
                Adapty.logShowPaywall(result.value) { }
        }
        billingManager = BillingManager(requireActivity()) { productId ->
            handleSuccess(productId)
        }

        val variant = sharedPref.getAdExtraData(configName, keyVariant)

        // Set dynamic subscription start date (trial end = today + trial days, or just today)
        setSubscriptionStartDate(view)

        billingManager.startConnection(onConnected = {
            if (variant == "Variant B") {
                fetchAndSetupVariantB(view)
            } else {
                fetchAndSetupVariantA(view)
            }
        })
    }

    private fun setSubscriptionStartDate(view: View) {
        // Show a placeholder date (today + 3 days) until billing resolves real trial length
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
    // Variant A — single plan (Annual baseline / feature screen)
    // ──────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantA(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)
        val productId = when (highlighted) {
            "weekly"  -> PaywallCatalog.ONBOARDING_WEEKLY
            "monthly" -> PaywallCatalog.ONBOARDING_MONTHLY
            else      -> PaywallCatalog.ONBOARDING_ANNUAL
        }

        billingManager.fetchProductDetails(listOf(productId)) { detailsMap ->
            val details = detailsMap[productId]

            if (details != null) {
                view.findViewById<TextView>(R.id.tvPrice)?.text =
                    PricingHelper.fullPriceLabel(details)

                val perMonth = PricingHelper.perMonthLabel(details)
                view.findViewById<TextView>(R.id.tvPerMonth)?.apply {
                    text       = if (perMonth.isNotEmpty()) "($perMonth)" else ""
                    visibility = if (perMonth.isNotEmpty()) View.VISIBLE else View.GONE
                }

                val trialDays = PricingHelper.freeTrialDays(details)
                view.findViewById<TextView>(R.id.btnContinue)?.text =
                    if (trialDays > 0) "Start Your $trialDays-Day Free Trial" else "Continue"

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
        // Variant A uses fragment_variant1_paywall — btnContinue sits inside a CardView
        // with cardCornerRadius="24dp", so match the shimmer to that radius.
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
    }

    // ──────────────────────────────────────────────────────────────
    // Variant B — dual plan (Monthly + Annual with badge)
    // ──────────────────────────────────────────────────────────────

    private fun fetchAndSetupVariantB(view: View) {
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)

        billingManager.fetchProductDetails(
            listOf(PaywallCatalog.ONBOARDING_MONTHLY, PaywallCatalog.ONBOARDING_ANNUAL)
        ) { detailsMap ->

            // Monthly card
            detailsMap[PaywallCatalog.ONBOARDING_MONTHLY]?.let { monthly ->
                view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text =
                    "${PricingHelper.formattedPrice(monthly)}\nBilled ${PricingHelper.billingPeriod(monthly).replaceFirstChar { it.uppercase() }}"
            }

            // Annual card
            detailsMap[PaywallCatalog.ONBOARDING_ANNUAL]?.let { annual ->
                val trialLabel = PricingHelper.freeTrialLabel(annual)
                val perMonth   = PricingHelper.perMonthLabel(annual)
                val price      = PricingHelper.formattedPrice(annual)

                view.findViewById<TextView>(R.id.tvAnnualLabel)?.text =
                    if (trialLabel.isNotEmpty()) "Annual\n$trialLabel" else "Annual"

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

        annualBadge?.visibility  = if (highlighted == "annual")  View.VISIBLE else View.GONE
        monthlyBadge?.visibility = if (highlighted == "monthly") View.VISIBLE else View.GONE

        applySelection(isAnnual = highlighted == "annual", rbMonthly, rbAnnual)

        rlMonthly?.setOnClickListener { applySelection(isAnnual = false, rbMonthly, rbAnnual) }
        rlAnnual?.setOnClickListener  { applySelection(isAnnual = true,  rbMonthly, rbAnnual) }

        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            val subId = if (rbAnnual?.isChecked == true)
                PaywallCatalog.ONBOARDING_ANNUAL else PaywallCatalog.ONBOARDING_MONTHLY
            billingManager.launchBillingFlow(subId)
        }
        // Variant B uses fragment_variant2_paywall — btnContinue is a plain Button
        // with backgroundTint (no custom drawable), Material default corner ~4dp.
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
        findNavController().navigate(R.id.action_onboardingPaywall_to_mainFragment)
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }
}