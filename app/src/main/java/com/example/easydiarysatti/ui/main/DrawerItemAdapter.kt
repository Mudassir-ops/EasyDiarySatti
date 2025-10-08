package com.example.easydiarysatti.ui.main

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.databinding.DrawerItemLayoutBinding
import com.example.easydiarysatti.domain.model.DrawerItem
import com.example.easydiarysatti.utills.setImage

class DrawerItemAdapter(
    private val onNoteItemClick: (DrawerItem) -> Unit
) : ListAdapter<DrawerItem, DrawerItemAdapter.NoteItemViewHolder>(DiffCallback) {

    inner class NoteItemViewHolder(private val binding: DrawerItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(drawerModel: DrawerItem) {
            binding.apply {
                tvDrawerItemTitle.text = drawerModel.title
                ivEditProfile.setImage(drawable = drawerModel.imgRes)
                ivEditProfile.backgroundTintList =
                    ColorStateList.valueOf(drawerModel.bgTint.toColorInt())
                itemView.setOnClickListener {
                    onNoteItemClick.invoke(drawerModel)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItemViewHolder {
        val binding = DrawerItemLayoutBinding.inflate(
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
        private val DiffCallback = object : DiffUtil.ItemCallback<DrawerItem>() {
            override fun areItemsTheSame(
                oldItem: DrawerItem,
                newItem: DrawerItem
            ): Boolean = oldItem.bgTint == newItem.bgTint

            override fun areContentsTheSame(
                oldItem: DrawerItem,
                newItem: DrawerItem
            ): Boolean = oldItem == newItem
        }
    }

}

