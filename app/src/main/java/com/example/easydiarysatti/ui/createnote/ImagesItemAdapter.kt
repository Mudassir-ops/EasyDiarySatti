package com.example.easydiarysatti.ui.createnote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ImageItemLayoutBinding
import com.example.easydiarysatti.databinding.NoteItemLayoutBinding
import com.example.easydiarysatti.loadImage

class ImagesItemAdapter(
    private val onNoteItemClick: (String) -> Unit
) : ListAdapter<String, ImagesItemAdapter.ImagesItemViewHolder>(DiffCallback) {

    inner class ImagesItemViewHolder(private val binding: ImageItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(noteEntity: String) {
            binding.ivNoteImage.loadImage(resourceString = noteEntity)
            binding.root.setOnClickListener {
                onNoteItemClick.invoke(noteEntity)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagesItemViewHolder {
        val binding = ImageItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImagesItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImagesItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean = oldItem == newItem

        }
    }
}

