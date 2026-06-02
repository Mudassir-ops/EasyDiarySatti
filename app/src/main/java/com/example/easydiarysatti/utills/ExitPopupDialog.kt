package com.example.easydiarysatti.utills

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.TextView
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Exit Popup Dialog — 3 visual variants.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  Variant A – SIMPLE       →  dialog_exit_simple.xml                     │
 * │  Variant B – WITH_RATING  →  exit_dialog_rating.xml                     │
 * │  Variant C – WITH_NATIVE  →  dialog_exit_native.xml                     │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * ── Why margins were ignored ───────────────────────────────────────────────
 * Setting the Dialog window to MATCH_PARENT width forces the window itself to
 * fill the full screen.  layout_marginStart/End on the root view then only
 * shrink the card visually but the window background still spans edge-to-edge,
 * making the margins appear to have no effect.
 *
 * Fix: set the window width to 90 % of screen width (via WindowManager params)
 * and remove the horizontal margins from the XML root view entirely.
 * The card now sits with proper breathing room on both sides.
 *
 * ── Remote Config / IAP JSON control ───────────────────────────────────────
 * Item name  : "exit_popup_config"   (lives in iapJson)
 * Fields:
 *   exit_popup_display        → "true" / "false"  (master toggle)
 *   exit_popup_layout_variant → "Variant A" | "Variant B" | "Variant C"
 *
 * ── Dismiss behaviour ──────────────────────────────────────────────────────
 *   Tapping outside the card  → dismiss  (stay in app)
 *   Variant A  btnStayInApp   → dismiss  (stay in app)
 *   Variant A  btnExitApp     → dismiss + onExit()
 *   Variant B  btnRateNow (≥4)→ dismiss + open Play Store
 *   Variant B  btnRateNow (<4)→ dismiss  (stay in app)
 *   Variant B  tvMaybeLater   → dismiss  (stay in app)
 *   Variant B  tvExitAnyway   → dismiss + onExit()
 *   Variant C  btnStayInApp   → dismiss  (stay in app)
 *   Variant C  btnExitApp     → dismiss + onExit()
 */
class ExitPopupDialog(
    context: Context,
    private val variant: Variant,
    private val nativeAd: NativeAd? = null,
    private val onExit: () -> Unit,
    private val onDismiss: (() -> Unit)? = null
) : Dialog(context) {

    enum class Variant { SIMPLE, WITH_RATING, WITH_NATIVE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutRes = when (variant) {
            Variant.SIMPLE      -> R.layout.dialog_exit_simple
            Variant.WITH_RATING -> R.layout.exit_dialog_rating
            Variant.WITH_NATIVE -> R.layout.dialog_exit_native
        }

        val view = LayoutInflater.from(context).inflate(layoutRes, null)
        setContentView(view)

        // ── FIX: set window width to 90% of screen width ──────────────────
        //
        // DO NOT use MATCH_PARENT here — that stretches the window edge-to-edge
        // and makes any layout_margin on the root view appear invisible.
        //
        // Instead we calculate 90% of the screen width and set it explicitly.
        // The XML root views no longer need layout_marginStart/End at all —
        // the 10% gap (5% each side) is handled entirely by the window size.
        //
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        window?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        val windowWidth = (displayMetrics.widthPixels * 0.90).toInt()

        window?.setLayout(windowWidth, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.CENTER)

        // Tapping outside the card dismisses without triggering onExit
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        when (variant) {

            // ── Variant A ──────────────────────────────────────────────────
            // Layout   : dialog_exit_simple.xml
            // Primary  : btnStayInApp  (filled teal)
            // Secondary: btnExitApp    (outlined)
            Variant.SIMPLE -> {
                view.findViewById<Button>(R.id.btnStayInApp)?.setOnClickListener {
                    dismissSafely()
                }
                view.findViewById<Button>(R.id.btnExitApp)?.setOnClickListener {
                    dismissSafely()
                    onExit()
                }
            }

            // ── Variant B ──────────────────────────────────────────────────
            // Layout: exit_dialog_rating.xml
            // IDs   : ratingBar, btnRateNow, tvMaybeLater, tvExitAnyway
            Variant.WITH_RATING -> {
                view.findViewById<Button>(R.id.btnRateNow)?.setOnClickListener {
                    val rating = view.findViewById<RatingBar>(R.id.ratingBar)?.rating ?: 0f
                    dismissSafely()
                    if (rating >= 4f) openPlayStore()
                }
                view.findViewById<TextView>(R.id.tvMaybeLater)?.setOnClickListener {
                    dismissSafely()
                }
                view.findViewById<TextView>(R.id.tvExitAnyway)?.setOnClickListener {
                    dismissSafely()
                    onExit()
                }
            }

            // ── Variant C ──────────────────────────────────────────────────
            // Layout: dialog_exit_native.xml
            // IDs   : btnExitApp (muted/left), btnStayInApp (teal/right),
            //         nativeAdContainer (FrameLayout),
            //         nativePlaceholderCard (inner CardView placeholder)
            Variant.WITH_NATIVE -> {
                view.findViewById<Button>(R.id.btnExitApp)?.setOnClickListener {
                    dismissSafely()
                    onExit()
                }
                view.findViewById<Button>(R.id.btnStayInApp)?.setOnClickListener {
                    dismissSafely()
                }

                val container = view.findViewById<FrameLayout>(R.id.nativeAdContainer)
                if (nativeAd != null && container != null) {
                    // Hide the static XML placeholder before injecting the real ad
                    container.findViewById<View>(R.id.nativePlaceholderCard)?.visibility =
                        View.GONE
                    val adSmallView = AdNativeSmallView(context)
                    adSmallView.setNativeAd(nativeAd)
                    container.addView(adSmallView)
                } else {
                    // Ad not loaded yet — hide the container, dialog still usable
                    container?.visibility = View.GONE
                }
            }
        }

        setOnDismissListener { onDismiss?.invoke() }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun openPlayStore() {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=${context.packageName}")
                )
            )
        }.onFailure {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://play.google.com/store/apps/details?id=${context.packageName}"
                    )
                )
            )
        }
    }

    private fun dismissSafely() {
        if (isShowing) dismiss()
    }

    // ── Companion (static factory) ─────────────────────────────────────────────

    companion object {
        fun show(
            context: Context,
            variant: Variant = Variant.SIMPLE,
            nativeAd: NativeAd? = null,
            onExit: () -> Unit,
            onDismiss: (() -> Unit)? = null
        ) = ExitPopupDialog(context, variant, nativeAd, onExit, onDismiss).show()
    }
}