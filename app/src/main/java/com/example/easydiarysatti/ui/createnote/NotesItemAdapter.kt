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
    private val onNoteItemClick:     (CreateNoteEntity) -> Unit,
    private val onNoteItemLongClick: (CreateNoteEntity) -> Unit,
    private val onFavClick:          (CreateNoteEntity) -> Unit,
    private val onDeleteClick:       (CreateNoteEntity) -> Unit,
    private val onMoreOptionClick:   (View, CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, RecyclerView.ViewHolder>(DiffCallback) {

    private var nativeAd:   NativeAd?      = null
    private var selectedIds: Set<Long>     = emptySet()

    // ✅ Optimistic overrides: noteId → isFavorite
    // Written instantly on toggle, cleared when real DiffUtil data arrives.
    private val favOverrides = mutableMapOf<Long, Boolean>()

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds = ids.toSet()
        notifyDataSetChanged()
    }

    /**
     * Called immediately after toggleFavorite() so the heart flips
     * without waiting for the DB → Flow → DiffUtil roundtrip.
     */
    fun updateFavoriteInstant(noteId: Long, newIsFavorite: Boolean) {
        favOverrides[noteId] = newIsFavorite

        // Find position and send a lightweight payload — only the heart redraws
        val dataList = currentList
        val dataIndex = dataList.indexOfFirst { it.noteId == noteId }
        if (dataIndex < 0) return
        val adapterPos = if (nativeAd != null && dataIndex >= AD_POSITION)
            dataIndex + 1 else dataIndex
        notifyItemChanged(adapterPos, PAYLOAD_FAV)
    }

    companion object {
        private const val TYPE_NOTE   = 1
        private const val TYPE_AD     = 2
        private const val AD_POSITION = 1

        // Payload object — signals that only the heart icon needs updating
        val PAYLOAD_FAV = Any()

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

    // ✅ Payload-aware bind — only updates the heart, skips full rebind
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_FAV) && holder is NoteItemViewHolder) {
            val actualPos = if (position > AD_POSITION && nativeAd != null) position - 1 else position
            val note      = getItem(actualPos)
            // Use override if present, else fall back to DB value
            val isFav     = favOverrides[note.noteId] ?: note.isFavorite
            holder.updateHeartIcon(isFav)
            return   // ← skip full bind entirely
        }
        // No payload or non-note holder → full bind
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NOTE) {
            val actualPosition = if (position > AD_POSITION && nativeAd != null) position - 1 else position
            val note           = getItem(actualPosition)
            val noteHolder     = holder as NoteItemViewHolder

            // ── Selection stroke ──────────────────────────────────────────
            val isSelected = selectedIds.contains(note.noteId)
            noteHolder.binding.cardRoot.apply {
                strokeWidth = if (isSelected)
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp) else 0
                strokeColor = if (isSelected)
                    ContextCompat.getColor(context, R.color.app_primary_color)
                else android.graphics.Color.TRANSPARENT
            }

            // ✅ Clear the override once the real DiffUtil data has arrived
            favOverrides.remove(note.noteId)

            noteHolder.bind(note)
        } else {
            (holder as AdViewHolder).bind(nativeAd)
        }
    }

    // ── NoteItemViewHolder ────────────────────────────────────────────────

    inner class NoteItemViewHolder(
        val binding: NoteItemLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(noteEntity: CreateNoteEntity) {
            // Check optimistic override first, fall back to DB value
            val isFav = favOverrides[noteEntity.noteId] ?: noteEntity.isFavorite
            updateHeartIcon(isFav)
            val isSelectionMode = selectedIds.isNotEmpty()
            binding.ivMoreOptions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.ivMoreOptions.isEnabled  = !isSelectionMode
            binding.ivFavoriteIndicator.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
            binding.ivFavoriteIndicator.isEnabled  = !isSelectionMode
            binding.noteEntity = noteEntity
            binding.executePendingBindings()

            binding.ivMoreOptions.setOnClickListener {
                onMoreOptionClick(it, noteEntity)
            }
            binding.viewForeground.setOnClickListener {
                onNoteItemClick(noteEntity)
            }
            binding.viewForeground.setOnLongClickListener {
                onNoteItemLongClick(noteEntity)
                true
            }
        }

        /** Updates ONLY the heart icon — called from both full bind and payload bind. */
        fun updateHeartIcon(isFavorite: Boolean) {
            binding.ivFavoriteIndicator.apply {
                imageTintList = null
                if (isFavorite) {
                    setImageResource(R.drawable.ic_heart_filled)
                    imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.amber)
                    )
                } else {
                    setImageResource(R.drawable.ic_heart_outline)
                    imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.tag_txt_color)
                    )
                }
            }
        }
    }
    fun hideAdSlot() {
        nativeAd = null
        notifyDataSetChanged()
    }

    // ── AdViewHolder ──────────────────────────────────────────────────────

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