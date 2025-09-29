package com.example.easydiarysatti.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.ItemDateHeaderBinding
import com.example.easydiarysatti.databinding.ItemSingleImageBinding

class MultiViewAdapter(
    private val items: List<LibraryItem>,
    private val onImageClick: (String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DATE = 0
        const val TYPE_IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
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
                ImageViewHolder(binding, onImageClick)
            }

            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LibraryItem.DateItem -> (holder as DateViewHolder).bind(item)
            is LibraryItem.ImagesItem -> (holder as ImageViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class DateViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LibraryItem.DateItem) {
            binding.tvDate.text = item.date
        }
    }

    class ImageViewHolder(
        private val binding: ItemSingleImageBinding,
        private val onImageClick: (String, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LibraryItem.ImagesItem) {
            Glide.with(binding.root.context)
                .load(item.imagePaths)
                .thumbnail(
                    Glide.with(binding.root.context)
                        .load(binding.root.context)
                        .override(200)
                )
                .into(binding.imageView)



            binding.tvTitle.text = "Satti"
            binding.imageView.setOnClickListener {
                //  onImageClick(item.imagePaths, item.date)
            }
        }
    }

}
