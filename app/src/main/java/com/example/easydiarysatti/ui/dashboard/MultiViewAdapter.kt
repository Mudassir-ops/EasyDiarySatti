package com.example.easydiarysatti.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.databinding.ItemDateHeaderBinding
import com.example.easydiarysatti.databinding.ItemSingleImageBinding

class MultiViewAdapter(
    private val onImageClick: (List<String>, String, Long) -> Unit // Update parameter
) : ListAdapter<LibraryItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val TYPE_DATE = 0
        const val TYPE_IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LibraryItem.DateItem -> TYPE_DATE
            is LibraryItem.ImagesItem -> TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
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
        when (val item = getItem(position)) {
            is LibraryItem.DateItem -> (holder as DateViewHolder).bind(item)
            is LibraryItem.ImagesItem -> (holder as ImageViewHolder).bind(item)
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


