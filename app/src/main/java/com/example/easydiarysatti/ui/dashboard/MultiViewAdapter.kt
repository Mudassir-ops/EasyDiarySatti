package com.example.easydiarysatti.ui.dashboard

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.databinding.ItemDateHeaderBinding
import com.example.easydiarysatti.databinding.ItemNativeAdContainerBinding
import com.example.easydiarysatti.databinding.ItemSingleImageBinding
import com.google.android.gms.ads.nativead.NativeAd

class MultiViewAdapter(
    private val onImageClick: (List<String>, String, Long) -> Unit // Update parameter
) : ListAdapter<LibraryItem, RecyclerView.ViewHolder>(DiffCallback()) {

    // 1. Add NativeAd property
    private var nativeAd: NativeAd? = null
    // Track if the 3-second delay has already passed for the current ad
    private var isShimmerComplete = false
    companion object {
        const val TYPE_DATE = 0
        const val TYPE_IMAGE = 1
        const val TYPE_AD = 2 // New Type
    }

    fun setNativeAd(ad: NativeAd) {
        this.nativeAd = ad
        this.isShimmerComplete = false // Reset for new ad content
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LibraryItem.DateItem -> TYPE_DATE
            is LibraryItem.ImagesItem -> TYPE_IMAGE
            is LibraryItem.AdItem -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_AD -> {
                val binding = ItemNativeAdContainerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AdViewHolder(binding)
            }
            TYPE_DATE -> {
                val binding = ItemDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateViewHolder(binding)
            }

            TYPE_IMAGE -> {
                val binding = ItemSingleImageBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ImageViewHolder(binding, onImageClick,)
            }

            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AdViewHolder -> holder.bind(nativeAd) // Pass the ad (could be null)
            is DateViewHolder -> holder.bind(getItem(position) as LibraryItem.DateItem)
            is ImageViewHolder -> holder.bind(getItem(position) as LibraryItem.ImagesItem)
        }
    }

    inner class AdViewHolder(val binding: ItemNativeAdContainerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: NativeAd?) {
            if (ad != null) {
                if (isShimmerComplete) {
                    // 3. Shimmer time finished: Show the ad immediately
                    showAdContent(ad)
                } else {
                    // 1. Ad is ready but we MUST show shimmer for 3-4 seconds first
                    showShimmer()

//                    Handler(Looper.getMainLooper()).postDelayed({
                        isShimmerComplete = true
                        // Ensure the holder is still visible/valid before updating
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            showAdContent(ad)
                        }
//                    }, 3500) // 3.5 seconds (the sweet spot between 3 and 4)
                }
            } else {
                // Ad not loaded at all: Show shimmer
                showShimmer()
            }
        }

        private fun showShimmer() {
            binding.flAdplaceholder.visibility = View.GONE
            binding.shimmerViewContainer.visibility = View.VISIBLE
            binding.shimmerViewContainer.startShimmer()
        }

        private fun showAdContent(ad: NativeAd) {
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.flAdplaceholder.visibility = View.VISIBLE

            val adView = AdNativeSmallView(binding.root.context)
            binding.flAdplaceholder.removeAllViews()
            binding.flAdplaceholder.addView(adView)
            adView.setNativeAd(ad)
        }
    }
    class DateViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LibraryItem.DateItem) {
            binding.tvDate.text = item.date
        }
    }

    class ImageViewHolder(
        private val binding: ItemSingleImageBinding,
        private val onImageClick: (List<String>, String, Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LibraryItem.ImagesItem) {
            // Load the first image as the representative cover
            Glide.with(binding.imageView.context)
                .load(item.imagePaths.firstOrNull())
                .thumbnail(0.1f)
                .centerCrop()
                .into(binding.imageView)

            binding.tvTitle.text = item.noteTitle
            binding.imageView.setOnClickListener {
                onImageClick(item.imagePaths, item.date, item.noteId)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
        override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
            return when {
                oldItem is LibraryItem.DateItem && newItem is LibraryItem.DateItem ->
                    oldItem.date == newItem.date
                oldItem is LibraryItem.ImagesItem && newItem is LibraryItem.ImagesItem ->
                    oldItem.noteId == newItem.noteId // Use noteId for better comparison
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean = oldItem == newItem
    }
}


