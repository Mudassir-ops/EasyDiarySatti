package com.example.easydiarysatti.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ItemFavoriteNoteBinding
import com.example.easydiarysatti.monthlyFormatDate

class FavoritesAdapter(
    private val onItemClick: (CreateNoteEntity) -> Unit,
    private val onFavClick: (CreateNoteEntity) -> Unit
) : ListAdapter<CreateNoteEntity, FavoritesAdapter.FavoriteViewHolder>(DiffCallback()) {

    inner class FavoriteViewHolder(private val binding: ItemFavoriteNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: CreateNoteEntity) {
            binding.apply {
                // Date  e.g. "01 Apr"
                tvDate.text = root.context.monthlyFormatDate(note.creationTime ?: 0L)

                // Title
                tvTitle.text = note.title.orEmpty()

                // Body preview (truncated by layout)
                tvBody.text = note.description.orEmpty()

                // Favourite icon — always filled/teal since this is the Favorites list
                ivFavorite.isSelected = note.isFavorite

                root.setOnClickListener { onItemClick(note) }
                ivFavorite.setOnClickListener { onFavClick(note)

                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<CreateNoteEntity>() {
        override fun areItemsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
            old.noteId == new.noteId
        override fun areContentsTheSame(old: CreateNoteEntity, new: CreateNoteEntity) =
            old == new
    }
}