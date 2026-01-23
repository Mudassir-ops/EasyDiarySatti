package com.example.easydiarysatti.ads.natives.presentation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.easydiarysatti.databinding.LayoutNativeLargeBinding
import com.google.android.gms.ads.nativead.NativeAd

class AdNativeLargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutNativeLargeBinding

    init {
        initView()
    }

    private fun initView() {
        binding = LayoutNativeLargeBinding.inflate(LayoutInflater.from(context), this, true)
    }

    // In AdNativeLargeView.kt
    fun setNativeAd(nativeAd: NativeAd) {
        binding.mtvLoadingAds.visibility = GONE
        binding.adAttribute.visibility = VISIBLE
        binding.adCallToAction.visibility = VISIBLE

        // Assigning views to the NativeAdView container
        binding.nativeAdView.mediaView = binding.adMediaView
        binding.nativeAdView.iconView = binding.adAppIcon
        binding.nativeAdView.headlineView = binding.adHeadline
        binding.nativeAdView.bodyView = binding.adBody
        binding.nativeAdView.callToActionView = binding.adCallToAction
        binding.nativeAdView.starRatingView = binding.adStars // Added Star Rating mapping

        // Filling up views
        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction
        binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)

        // Handle Star Rating visibility
        if (nativeAd.starRating != null) {
            binding.adStars.rating = nativeAd.starRating!!.toFloat()
            binding.adStars.visibility = VISIBLE
        } else {
            binding.adStars.visibility = GONE
        }

        // Validating icon and CTA visibility
        binding.adAppIcon.isVisible = nativeAd.icon?.drawable != null
        binding.adCallToAction.isVisible = !nativeAd.callToAction.isNullOrEmpty()

        visibility = VISIBLE
        binding.nativeAdView.setNativeAd(nativeAd) // Finalize binding
    }

    fun clearView() {
        binding.mtvLoadingAds.visibility = VISIBLE
        binding.adAttribute.visibility = GONE
        binding.adCallToAction.visibility = GONE
    }
}