package com.example.easydiarysatti.paywalls

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

/**
 * Remove Ads dialog — triggered by:
 *   • Tapping the "Remove Ads" pro card in the drawer
 *   • Tapping X on the (N+1)th interstitial ad (N = remove_ads_inter_ad_cross_ipu value)
 *
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │  VARIANT  │ LAYOUT                        │ NATIVE AD                │
 * ├──────────────────────────────────────────────────────────────────────┤
 * │  "1"      │ dialog_remove_ads_no_native    │ None  — simple popup     │
 * │  "2"      │ dialog_remove_ads              │ Medium native ad inside   │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * Remote Config (inside remove_ads_inter_ad_cross_ipu iapJson item):
 *   Key: remove_ads_popup_layout_variant  → "1" | "2"    Default: "1"
 *
 * Remote Config (inside iaaJson):
 *   Key: remove_ads_native                → show: true/false
 *   Native ad unit: ca-app-pub-6929888913467755/3273373747
 */
@AndroidEntryPoint
class RemoveAdsDialog : DialogFragment() {

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    private val viewModelNative by viewModels<ViewModelNative>()

    private var variant: String        = "1"
    private var price: String          = ""
    private var onRemoveAds: (() -> Unit)? = null
    private var onDialogDismissed: (() -> Unit)? = null   // called on any close (X button or back)

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    //    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        return if (variant == "2") {
//            inflater.inflate(R.layout.dialog_remove_ads, container, false)
//        } else {
//            inflater.inflate(R.layout.dialog_remove_ads_no_native, container, false)
//        }
//    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutRes = when (variant) {
            "Variant B" -> R.layout.dialog_remove_ads           // with native ad
            else        -> R.layout.dialog_remove_ads_no_native // Variant A or any unknown
        }
        val view = inflater.inflate(layoutRes, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // FIX: was checking variant == "2" but onCreateView uses "Variant B" / "Variant A".
        // Mismatch meant Variant B always ran setupVariant1() — tvPrice never set,
        // native ad never loaded, price never shown.
        if (variant == "Variant B") setupVariant2(view) else setupVariant1(view)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDialogDismissed?.invoke()
    }

    override fun onDestroyView() {
        view?.findViewById<View>(R.id.btnRemoveAds)?.stopShimmer()
        super.onDestroyView()
    }

    // ── Variant 1 — simple popup, no native ───────────────────────────────────
    // Layout: dialog_remove_ads_no_native.xml
    // Views : ivHero, tvTitle, tvSubtitle, cvPlan, tvPriceAmount,
    //         btnRemoveAds, llFooter, tvDisclaimer, btnClose
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupVariant1(view: View) {
        view.findViewById<TextView>(R.id.tvPriceAmount)?.text =
            price.ifEmpty { "Remove Ads - Lifetime" }

        view.findViewById<View>(R.id.btnRemoveAds)?.setOnClickListener {
            dismiss(); onRemoveAds?.invoke()
        }
        view.findViewById<View>(R.id.btnRemoveAds)?.startShimmerDp(28f)
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dismiss() }
    }

    // ── Variant 2 — popup with medium native ad ───────────────────────────────
    // Layout: dialog_remove_ads.xml
    // Views : nativeAdContainer (FrameLayout), tvPrice, btnRemoveAds, btnClose
    //
    // Native ad:
    //   iaaJson key : remove_ads_native
    //   Ad unit ID  : ca-app-pub-6929888913467755/3273373747
    //   Remote toggle: show field in remove_ads_native iaaJson entry (true/false)
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupVariant2(view: View) {
        view.findViewById<TextView>(R.id.tvPrice)?.text =
            price.ifEmpty { "Remove Ads" }

        // Native ad — controlled by remote toggle via iaaJson "show" field
        val nativeEnabled  = sharedPref.getAdShowStatus("remove_ads_native")
        val nativeContainer = view.findViewById<FrameLayout>(R.id.nativeAdContainer)
        if (nativeEnabled && nativeContainer != null) {
            nativeContainer.visibility = View.VISIBLE
            viewModelNative.loadNativeAd(NativeAdKey.REMOVE_ADS_NATIVE)
            viewModelNative.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
                adMap[NativeAdKey.REMOVE_ADS_NATIVE]?.let { nativeAd ->
                    // bind nativeAd into nativeContainer using your NativeAdView
                    nativeContainer.removeAllViews()
                    val adView = AdNativeSmallView(requireContext())  // handles all inflation internally
                    nativeContainer.addView(adView)
                    adView.setNativeAd(nativeAd)                      // binds all assets + registers views
//                    nativeContainer.visibility = View.VISIBLE
                    nativeContainer.visibility = if (nativeAd != null) View.VISIBLE else View.GONE
                }
            }
        } else {
            nativeContainer?.visibility = View.GONE
        }


        view.findViewById<View>(R.id.btnRemoveAds)?.setOnClickListener {
            dismiss(); onRemoveAds?.invoke()
        }
        view.findViewById<View>(R.id.btnRemoveAds)?.startShimmerDp(28f)
        view.findViewById<View>(R.id.btnClose)?.setOnClickListener { dismiss() }
    }

    // ── Companion ─────────────────────────────────────────────────────────────
// In the companion object, ensure the default parameter is updated
    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            variant: String = "Variant A", // Updated default
            price: String = "",
            onRemoveAds: () -> Unit,
            onDismissed: (() -> Unit)? = null
        ) {
            // Check for existing dialog to prevent double-showing
            (fragmentManager.findFragmentByTag("RemoveAdsDialog") as? RemoveAdsDialog)
                ?.dismissAllowingStateLoss()

            if (fragmentManager.isStateSaved) return

            RemoveAdsDialog().apply {
                this.variant = variant
                this.price = price
                this.onRemoveAds = onRemoveAds
                this.onDialogDismissed = onDismissed
            }.showNow(fragmentManager, "RemoveAdsDialog")
        }
    }
}