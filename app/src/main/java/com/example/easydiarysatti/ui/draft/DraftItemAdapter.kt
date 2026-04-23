package com.example.easydiarysatti.ui.draft

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ItemDraftNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DraftItemAdapter(
    private val onEditClick:   (CreateNoteEntity) -> Unit,
    private val onDeleteClick: (CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, DraftItemAdapter.DraftViewHolder>(DiffCallback) {

    // ── ViewHolder ────────────────────────────────────────────────────────────

    inner class DraftViewHolder(
        private val binding: ItemDraftNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(draft: CreateNoteEntity) {
            binding.tvDraftTitle.text =
                draft.title?.takeIf { it.isNotBlank() } ?: "Untitled"

            binding.tvLastEdited.text = formatRelativeTime(draft.creationTime)

            binding.ivEditDraft.setOnClickListener   { onEditClick(draft)   }
            binding.ivDeleteDraft.setOnClickListener { onDeleteClick(draft) }

            // Whole row tap also opens the draft for editing
            binding.root.setOnClickListener { onEditClick(draft) }
        }
    }

    // ── Adapter overrides ─────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val binding = ItemDraftNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // ── DiffUtil ──────────────────────────────────────────────────────────────

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CreateNoteEntity>() {
            override fun areItemsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
                old.noteId == new.noteId

            override fun areContentsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
                old == new
        }

        /**
         * Converts a UTC timestamp (Long) to a human-readable relative string:
         *   "Just now", "X minutes ago", "X hours ago", "X days ago"
         *
         * Falls back to a short absolute date for anything older than 7 days.
         *
         * @param timestamp epoch-millis (same unit stored in CreateNoteEntity.creationTime)
         */
        fun formatRelativeTime(timestamp: Long): String {
            if (timestamp <= 0L) return ""
            val now   = System.currentTimeMillis()
            val delta = now - timestamp

            val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
            val hours   = TimeUnit.MILLISECONDS.toHours(delta)
            val days    = TimeUnit.MILLISECONDS.toDays(delta)

            return when {
                minutes < 1    -> "Just now"
                minutes < 60   -> "Last edited: $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                hours   < 24   -> "Last edited: $hours ${if (hours == 1L) "hour" else "hours"} ago"
                days    < 7    -> "Last edited: $days ${if (days == 1L) "day" else "days"} ago"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    "Last edited: ${sdf.format(Date(timestamp))}"
                }
            }
        }
    }
}