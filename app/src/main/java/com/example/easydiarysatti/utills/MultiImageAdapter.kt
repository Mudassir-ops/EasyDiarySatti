package com.example.easydiarysatti.utills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.easydiarysatti.R

class MultiImageAdapter(
    private val items: List<Int?>,
    private val onUploadClick: () -> Unit,
    private val onImageClick: (Int?) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    companion object {
        private const val TYPE_UPLOAD = 0
        private const val TYPE_IMAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_UPLOAD else TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_UPLOAD -> {
                val view = inflater.inflate(R.layout.bg_item_pick_layout, parent, false)
                UploadViewHolder(view)
            }

            else -> {
                val view = inflater.inflate(R.layout.bg_item_layout, parent, false)
                ImageViewHolder(view)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UploadViewHolder -> holder.bind(onUploadClick)
            is ImageViewHolder -> holder.bind(items[position], onImageClick)
        }
    }

    inner class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val uploadView =
            itemView.findViewById<View>(R.id.iv_upload)

        fun bind(onUploadClick: () -> Unit) {
            uploadView.setOnClickListener { onUploadClick() }
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView =
            itemView.findViewById<AppCompatImageView>(R.id.iv_bg)

        fun bind(item: Int?, onImageClick: (Int) -> Unit) {
            Glide.with(itemView.context)
                .load(item)
                .transform(RoundedCorners(24))
                .into(imageView)
            imageView.setOnClickListener {
                onImageClick(item)
                onItemClick?.invoke(bindingAdapterPosition)
            }
        }
    }
}
