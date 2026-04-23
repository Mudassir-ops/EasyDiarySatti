package com.example.easydiarysatti.ui.createnote

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ItemNativeAdContainerBinding
import com.example.easydiarysatti.databinding.NoteItemLayoutBinding
import com.google.android.gms.ads.nativead.NativeAd

class NotesItemAdapter(
    private val onNoteItemClick:  (CreateNoteEntity) -> Unit,
    private val onNoteItemLongClick: (CreateNoteEntity) -> Unit,
    private val onFavClick:       (CreateNoteEntity) -> Unit,
    private val onDeleteClick:    (CreateNoteEntity) -> Unit,
    private val onMoreOptionClick: (View, CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, RecyclerView.ViewHolder>(DiffCallback) {

    private var nativeAd: NativeAd? = null
    private var selectedIds: Set<Long> = emptySet()

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds = ids.toSet()
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_NOTE   = 1
        private const val TYPE_AD     = 2
        private const val AD_POSITION = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<CreateNoteEntity>() {
            override fun areItemsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
                old.noteId == new.noteId
            override fun areContentsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
                old == new
        }
    }

    override fun getItemViewType(position: Int) =
        if (position == AD_POSITION && nativeAd != null) TYPE_AD else TYPE_NOTE

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        return if (nativeAd != null && count >= 1) count + 1 else count
    }

    fun setNativeAd(ad: NativeAd) {
        nativeAd = ad
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_AD) {
            AdViewHolder(ItemNativeAdContainerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
        } else {
            NoteItemViewHolder(NoteItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NOTE) {
            val actualPosition = if (position > AD_POSITION) position - 1 else position
            val note           = getItem(actualPosition)
            val noteHolder     = holder as NoteItemViewHolder

            // ── Selection stroke ──────────────────────────────────────────────
            val isSelected = selectedIds.contains(note.noteId)
            noteHolder.binding.cardRoot.apply {
                strokeWidth = if (isSelected)
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp) else 0
                strokeColor = if (isSelected)
                    ContextCompat.getColor(context, R.color.app_primary_color)
                else android.graphics.Color.TRANSPARENT
            }

            noteHolder.bind(note)
        } else {
            (holder as AdViewHolder).bind(nativeAd)
        }
    }

    // ── NoteItemViewHolder ────────────────────────────────────────────────────

    inner class NoteItemViewHolder(
        val binding: NoteItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(noteEntity: CreateNoteEntity) {
            binding.noteEntity = noteEntity
            binding.executePendingBindings()

            // ── Heart icon: set drawable + tint atomically ────────────────────
            // Prevents the src/tint race condition from data binding where the
            // old tint can stick on the new drawable during a list diff rebind.
//            binding.ivFavoriteIndicator.apply {
//                setImageResource(
//                    if (noteEntity.isFavorite) R.drawable.ic_heart_filled
//                    else R.drawable.ic_heart_outline
//                )
//                imageTintList = ColorStateList.valueOf(
//                    ContextCompat.getColor(
//                        context,
//                        if (noteEntity.isFavorite) R.color.amber else R.color.tag_txt_color
//                    )
//                )
//                // Wire click — was missing; previously only swipe triggered toggleFavorite
////                setOnClickListener { onFavClick(noteEntity) }
//            }

            // ── 3-dots: opens NoteItemOptionsBottomSheet ──────────────────────
            binding.ivMoreOptions.setOnClickListener {
                onMoreOptionClick(it, noteEntity)
            }
            if (noteEntity.isFavorite) {
                binding.ivFavoriteIndicator.setImageResource(R.drawable.ic_heart_filled) // Your filled icon
                binding.ivFavoriteIndicator.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.amber) // Your #FBC401
                )
            } else {
                binding.ivFavoriteIndicator.setImageResource(R.drawable.ic_heart_outline) // Your empty icon
                binding.ivFavoriteIndicator.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.tag_txt_color)
                )
            }
            // ── Normal tap ────────────────────────────────────────────────────
            binding.viewForeground.setOnClickListener {
                onNoteItemClick(noteEntity)
            }

            // ── Long press → multi-select ─────────────────────────────────────
            binding.viewForeground.setOnLongClickListener {
                onNoteItemLongClick(noteEntity)
                true
            }
        }
    }

    // ── AdViewHolder ──────────────────────────────────────────────────────────

    inner class AdViewHolder(
        val binding: ItemNativeAdContainerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: NativeAd?) {
            if (ad != null) {
                binding.shimmerViewContainer.apply { stopShimmer(); visibility = View.GONE }
                binding.flAdplaceholder.apply {
                    visibility = View.VISIBLE
                    removeAllViews()
                    addView(AdNativeSmallView(binding.root.context).also { it.setNativeAd(ad) })
                }
            } else {
                binding.shimmerViewContainer.apply { startShimmer(); visibility = View.VISIBLE }
                binding.flAdplaceholder.visibility = View.GONE
            }
        }
    }
}