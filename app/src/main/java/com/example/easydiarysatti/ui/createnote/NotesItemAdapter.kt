package com.example.easydiarysatti.ui.createnote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.NoteItemLayoutBinding

class NotesItemAdapter(
    private val onNoteItemClick: (CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, NotesItemAdapter.NoteItemViewHolder>(DiffCallback) {

    inner class NoteItemViewHolder(private val binding: NoteItemLayoutBinding) :
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
        val binding = NoteItemLayoutBinding.inflate(
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

