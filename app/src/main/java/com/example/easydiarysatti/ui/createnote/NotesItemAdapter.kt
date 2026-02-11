package com.example.easydiarysatti.ui.createnote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ItemNativeAdContainerBinding
import com.example.easydiarysatti.databinding.NoteItemLayoutBinding
import com.google.android.gms.ads.nativead.NativeAd

class NotesItemAdapter(
    private val onNoteItemClick: (CreateNoteEntity) -> Unit,
    private val onNoteItemLongClick: (CreateNoteEntity) -> Unit,
    private val onFavClick: (CreateNoteEntity) -> Unit,
    private val onDeleteClick: (CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, RecyclerView.ViewHolder>(DiffCallback) {

    private var nativeAd: NativeAd? = null

    companion object {
        private const val TYPE_NOTE = 1
        private const val TYPE_AD = 2
        private const val AD_POSITION = 1 // 2nd item

        private val DiffCallback = object : DiffUtil.ItemCallback<CreateNoteEntity>() {
            override fun areItemsTheSame(oldItem: CreateNoteEntity, newItem: CreateNoteEntity) = oldItem.noteId == newItem.noteId
            override fun areContentsTheSame(oldItem: CreateNoteEntity, newItem: CreateNoteEntity) = oldItem == newItem
        }
    }



    override fun getItemViewType(position: Int): Int {
        // Show ad only if we have data and an ad is loaded
        return if (position == AD_POSITION && nativeAd != null) TYPE_AD else TYPE_NOTE
    }

    // NotesItemAdapter.kt

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        // Optimization: Only show ad if there are at least 1-2 notes
        // to ensure the ad doesn't look lonely or break the layout
        return if (nativeAd != null && count >= 1) count + 1 else count
    }

    fun setNativeAd(ad: NativeAd) {
        this.nativeAd = ad
        // Use notifyDataSetChanged or notifyItemInserted if the count actually changed
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val binding = ItemNativeAdContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AdViewHolder(binding)
        } else {
            val binding = NoteItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            NoteItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdViewHolder) {
            nativeAd?.let { holder.bind(it) }
        } else if (holder is NoteItemViewHolder) {
            // Adjust index if we are past the ad position
            val actualPosition = if (nativeAd != null && position > AD_POSITION) position - 1 else position
            holder.bind(getItem(actualPosition))
        }
    }

    // --- ViewHolders ---

    inner class NoteItemViewHolder(val binding: NoteItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(noteEntity: CreateNoteEntity) {
            binding.noteEntity = noteEntity
            binding.viewForeground.setOnClickListener { onNoteItemClick(noteEntity) }
            binding.executePendingBindings()
        }
    }

    inner class AdViewHolder(val binding: ItemNativeAdContainerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ad: NativeAd?) {
            if (ad != null) {
                // 1. Hide the Shimmer
                binding.shimmerViewContainer.apply {
                    stopShimmer()
                    visibility = View.GONE
                }

                // 2. Show and Fill Placeholder
                binding.flAdplaceholder.apply {
                    visibility = View.VISIBLE
                    removeAllViews()
                    val adView = AdNativeSmallView(binding.root.context)
                    addView(adView)
                    adView.setNativeAd(ad)
                }
            } else {
                // 3. Keep Shimmer visible if ad is null
                binding.shimmerViewContainer.apply {
                    startShimmer()
                    visibility = View.VISIBLE
                }
                binding.flAdplaceholder.visibility = View.GONE
            }
        }
    }
}