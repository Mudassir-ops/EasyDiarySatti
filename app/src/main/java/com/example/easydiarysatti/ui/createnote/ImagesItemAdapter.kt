package com.example.easydiarysatti.ui.createnote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.databinding.ImageItemLayoutBinding
import com.example.easydiarysatti.loadAdaptiveImage

class ImagesItemAdapter(
    private val onDeleteItemClick: (String) -> Unit,
    private val fromPreview: Boolean,
    private val imagesCount: (Int) -> Unit,
) : ListAdapter<String, ImagesItemAdapter.ImagesItemViewHolder>(DiffCallback) {

    inner class ImagesItemViewHolder(private val binding: ImageItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(noteEntity: String) {
            binding.ivNoteImage.loadAdaptiveImage(noteEntity)
            if (fromPreview) {
                binding.viewEdit.visibility = View.GONE
                binding.icEdit.visibility = View.GONE
                binding.icDelete.visibility = View.GONE
            }

            binding.icDelete.setOnClickListener {
                onDeleteItemClick.invoke(noteEntity)
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

    override fun submitList(list: List<String>?) {
        super.submitList(list)
        imagesCount.invoke(list?.size ?: 0)
    }
}

