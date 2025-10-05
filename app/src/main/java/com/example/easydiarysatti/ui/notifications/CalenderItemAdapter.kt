package com.example.easydiarysatti.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.CalenderNoteItemLayoutBinding
import com.example.easydiarysatti.databinding.NoteItemLayoutBinding

class CalenderItemAdapter(
    private val onNoteItemClick: (CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, CalenderItemAdapter.NoteItemViewHolder>(DiffCallback) {

    inner class NoteItemViewHolder(private val binding: CalenderNoteItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(noteEntity: CreateNoteEntity) {
            binding.noteEntity = noteEntity
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                onNoteItemClick.invoke(noteEntity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItemViewHolder {
        val binding = CalenderNoteItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CreateNoteEntity>() {
            override fun areItemsTheSame(
                oldItem: CreateNoteEntity,
                newItem: CreateNoteEntity
            ): Boolean = oldItem.noteId == newItem.noteId

            override fun areContentsTheSame(
                oldItem: CreateNoteEntity,
                newItem: CreateNoteEntity
            ): Boolean = oldItem == newItem
        }
    }
}

