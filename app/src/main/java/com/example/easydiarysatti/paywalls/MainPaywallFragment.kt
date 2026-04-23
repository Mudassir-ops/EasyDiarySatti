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
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.privacyPolicyUrl
import com.example.easydiarysatti.termsUrl
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainPaywallFragment : Fragment() {

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    private lateinit var billingManager: BillingManager

    private val configName     = "main_paywall_config"
    private val keyHighlighted = "main_paywall_screen_highlighted_plan"
    private val keyPlanType    = "main_paywall_screen_plan_type"

    // Read args once so every function can use them
    private val fromCreateNote   by lazy { arguments?.getBoolean(ARG_FROM_CREATE_NOTE,   false) ?: false }
    private val fromRewardedGate by lazy { arguments?.getBoolean(ARG_FROM_REWARDED_GATE, false) ?: false }

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

        billingManager.startConnection(onConnected = { setupUI(view) })
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
    }

    /**
     * Close the paywall and return to the correct screen.
     *
     * Cases:
     *
     * 1. fromRewardedGate = true  (opened via RewardedGateDialog "Subscribe Now" from CreateNotesFragment)
     *    → User has NOT saved yet. Must go back to CreateNotesFragment, NOT Home.
     *    → Just pop this fragment. Inner nav is still on CreateNotesFragment — do nothing else.
     *
     * 2. fromCreateNote = true (opened directly from Save button flow, quota exceeded)
     *    → User was on CreateNotesFragment. Must stay there on close (did not purchase).
     *    → Just pop this fragment. Inner nav is still on CreateNotesFragment — do nothing else.
     *
     * 3. Neither flag set (opened from Home / premium icon / drawer)
     *    → Just pop this fragment. Inner nav stays wherever it was.
     *
     * In ALL cross-button cases we simply pop back — we never forcibly navigate to Home.
     * Home navigation only happens after a successful PURCHASE (handleSuccess).
     */
    private fun closePaywall() {
        android.util.Log.d(
            "PaywallNav",
            "closePaywall() fromCreateNote=$fromCreateNote fromRewardedGate=$fromRewardedGate — popping back stack"
        )
        // Simply pop. MainFragment.setupOuterNavPaywallListener handles inner-nav restore.
        // If the inner nav was reset during view recreation, MainFragment.onViewCreated
        // checks paywallOpenedFromCreateNote and re-navigates to CreateNotesFragment.
        findNavController().popBackStack()
    }

    private fun setupUI(view: View) {
        val planType    = sharedPref.getAdExtraData(configName, keyPlanType)
        val highlighted = sharedPref.getAdExtraData(configName, keyHighlighted)

        fetchAndSetupMultiPlan(view, highlighted)

    }

//    private fun fetchAndSetupSinglePlan(view: View, highlighted: String) {
//        val productId = when (highlighted) {
//            "weekly"  -> PaywallCatalog.MAIN_WEEKLY
//            "monthly" -> PaywallCatalog.MAIN_MONTHLY
//            else      -> PaywallCatalog.MAIN_ANNUAL
//        }
//        billingManager.fetchProductDetails(listOf(productId)) { detailsMap ->
//            detailsMap[productId]?.let { details ->
//                view.findViewById<TextView>(R.id.tvPrice)?.text = PricingHelper.fullPriceLabel(details)
//                val perMonth = PricingHelper.perMonthLabel(details)
//                view.findViewById<TextView>(R.id.tvPerMonth)?.apply {
//                    text = if (perMonth.isNotEmpty()) "($perMonth)" else ""
//                    visibility = if (perMonth.isNotEmpty()) View.VISIBLE else View.GONE
//                }
//
//                val trialDays = PricingHelper.freeTrialDays(details)
//                view.findViewById<TextView>(R.id.btnContinue)?.text =
//                    if (trialDays > 0) "Start Your $trialDays-Day Free Trial" else "Continue"
//            }
//            view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
//                billingManager.launchBillingFlow(productId)
//            }
//        }
//    }

    private fun fetchAndSetupMultiPlan(view: View, highlighted: String) {
        billingManager.fetchProductDetails(
            listOf(PaywallCatalog.MAIN_MONTHLY, PaywallCatalog.MAIN_ANNUAL)
        ) { detailsMap ->
            detailsMap[PaywallCatalog.MAIN_MONTHLY]?.let { monthly ->
                view.findViewById<TextView>(R.id.tvMonthlyPrice)?.text =
                    "${PricingHelper.formattedPrice(monthly)}\nBilled ${PricingHelper.billingPeriod(monthly).replaceFirstChar { it.uppercase() }}"
            }
            detailsMap[PaywallCatalog.MAIN_ANNUAL]?.let { annual ->
                val trialLabel = PricingHelper.freeTrialLabel(annual)
                val perMonth   = PricingHelper.perMonthLabel(annual)
                val price      = PricingHelper.formattedPrice(annual)
                view.findViewById<TextView>(R.id.tvAnnualLabel)?.text =
                    if (trialLabel.isNotEmpty()) "Annual\n$trialLabel" else "Annual"
                view.findViewById<TextView>(R.id.tvAnnualPrice)?.text =
                    if (perMonth.isNotEmpty()) "$price\nJust $perMonth" else price
            }
            setupMultiPlanClickListeners(view, highlighted)
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

        val selectMonthly = View.OnClickListener { applySelection(false, rbMonthly, rbAnnual) }
        val selectAnnual  = View.OnClickListener { applySelection(true,  rbMonthly, rbAnnual) }
        rlMonthly?.setOnClickListener(selectMonthly)
        for (i in 0 until (rlMonthly?.childCount ?: 0)) rlMonthly?.getChildAt(i)?.setOnClickListener(selectMonthly)
        rlAnnual?.setOnClickListener(selectAnnual)
        for (i in 0 until (rlAnnual?.childCount ?: 0)) rlAnnual?.getChildAt(i)?.setOnClickListener(selectAnnual)

        view.findViewById<View>(R.id.btnContinue)?.setOnClickListener {
            val subId = if (rbAnnual?.isChecked == true) PaywallCatalog.MAIN_ANNUAL else PaywallCatalog.MAIN_MONTHLY
            billingManager.launchBillingFlow(subId)
        }
        view.findViewById<View>(R.id.btnContinue)?.startShimmerDp(4f)
    }
    private fun applySelection(isAnnual: Boolean, rbM: RadioButton?, rbA: RadioButton?) {
        rbA?.isChecked = isAnnual
        rbM?.isChecked = !isAnnual
    }
//    private fun applySelection(isAnnual: Boolean, rbM: RadioButton?, rbA: RadioButton?) {
//        rbM?.isClickable = false
//        rbM?.isFocusable = false
//        rbA?.isClickable = false
//        rbA?.isFocusable = false
//        rbA?.isChecked = isAnnual
//        rbM?.isChecked = !isAnnual
//        // android:button="@null" + custom background needs explicit state refresh
//        rbA?.refreshDrawableState()
//        rbM?.refreshDrawableState()
//    }

    private fun handleSuccess(productId: String) {
        sharedPref.isAppPurchased = true
        val event = when (productId) {
            PaywallCatalog.MAIN_WEEKLY  -> "main_paywall_weekly_purchased"
            PaywallCatalog.MAIN_MONTHLY -> "main_paywall_monthly_purchased"
            PaywallCatalog.MAIN_ANNUAL  -> "main_paywall_annual_purchased"
            else                        -> "main_paywall_purchased"
        }
        FirebaseAnalytics.getInstance(requireContext()).logEvent(event, null)
        // After purchase: pop paywall, then MainFragment.setupOuterNavPaywallListener
        // will pop inner nav to homeFragment (because innerDestBeforePaywall == homeFragment
        // was set during the post-save paywall open path).
        // For RewardedGate path (fromRewardedGate=true), innerDestBeforePaywall ==
        // createNotesFragment, so listener does nothing and user stays on CreateNotesFragment.
        closePaywall()
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnContinue)?.stopShimmer()
        billingManager.endConnection()
        super.onDestroyView()
    }

    companion object {
        /** Pass this argument as true when opening paywall from CreateNotesFragment save flow */
        const val ARG_FROM_CREATE_NOTE   = "from_create_note"

        /**
         * Pass this argument as true when opening paywall from the RewardedGateDialog
         * "Subscribe Now" button. In this case closePaywall() must NOT redirect inner nav
         * to Home — the user has not saved yet and should return to CreateNotesFragment.
         */
        const val ARG_FROM_REWARDED_GATE = "from_rewarded_gate"

        /**
         * Create a Bundle with the correct flags to pass when navigating to this fragment.
         *
         * Usage in MainFragment.onSubscribe:
         *   findNavController().navigate(
         *       R.id.action_..._to_mainPaywallFragment,
         *       MainPaywallFragment.args(fromCreateNote = true, fromRewardedGate = true)
         *   )
         */
        fun args(
            fromCreateNote: Boolean   = false,
            fromRewardedGate: Boolean = false
        ): Bundle = Bundle().apply {
            putBoolean(ARG_FROM_CREATE_NOTE,   fromCreateNote)
            putBoolean(ARG_FROM_REWARDED_GATE, fromRewardedGate)
        }
    }
}