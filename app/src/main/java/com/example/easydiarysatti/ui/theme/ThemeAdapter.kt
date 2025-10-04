package com.example.easydiarysatti.ui.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.databinding.ItemThemeBinding

class ThemeAdapter(
    private val themes: List<Int>,
    private val onThemeClick: (Int) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ThemeViewHolder(val binding: ItemThemeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(themeRes: Int, isSelected: Boolean) {
            binding.themeResId = themeRes
            binding.executePendingBindings()
            binding.root.setOnClickListener {
                val oldPos = selectedPosition
                selectedPosition = bindingAdapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onThemeClick(themeRes)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(themes[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = themes.size
}
