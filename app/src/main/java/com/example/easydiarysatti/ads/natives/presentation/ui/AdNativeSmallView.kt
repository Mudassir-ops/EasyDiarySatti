package com.example.easydiarysatti.ads.natives.presentation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.easydiarysatti.databinding.LayoutNativeSmallBinding
import com.google.android.gms.ads.nativead.NativeAd

class AdNativeSmallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutNativeSmallBinding

    init {
        initView()
    }

    private fun initView() {
        binding = LayoutNativeSmallBinding.inflate(LayoutInflater.from(context), this, true)
    }
    fun setNativeAd(nativeAd: NativeAd) {
        // 1. Hide the loading text immediately
        binding.mtvLoadingAds.visibility = GONE

        // 2. Map the NativeAdView parts
        binding.nativeAdView.headlineView = binding.adHeadline
        binding.nativeAdView.bodyView = binding.adBody
        binding.nativeAdView.iconView = binding.adAppIcon
        binding.nativeAdView.starRatingView = binding.adStars // Added mapping for stars
        binding.nativeAdView.callToActionView = binding.adCallToAction

        // 3. Set the Data
        binding.adHeadline.text = nativeAd.headline
        binding.adBody.text = nativeAd.body
        binding.adCallToAction.text = nativeAd.callToAction
        binding.adAppIcon.setImageDrawable(nativeAd.icon?.drawable)


        // 4. Show the Badge and Button
        binding.adAttribute.visibility = VISIBLE
        binding.adCallToAction.visibility = VISIBLE

        // 5. Handle Star Rating logic
        if (nativeAd.starRating != null) {
            binding.adStars.visibility = VISIBLE
            binding.adStars.rating = nativeAd.starRating!!.toFloat()
        } else {
            binding.adStars.visibility = GONE
        }

        // 6. Finalize the ad registration
        binding.nativeAdView.setNativeAd(nativeAd)
        this.visibility = VISIBLE
    }
}