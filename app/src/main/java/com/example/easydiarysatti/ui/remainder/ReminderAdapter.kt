package com.example.easydiarysatti.ui.remainder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.databinding.ItemReminderBinding

class ReminderAdapter(
    private val onItemClick: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, ReminderAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataModel = getItem(position)
        with(holder.binding) {
            icCross.setOnClickListener { onItemClick.invoke(dataModel) }
        }
    }

    class ViewHolder(val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ReminderEntity>() {
            override fun areItemsTheSame(
                oldItem: ReminderEntity, newItem: ReminderEntity
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ReminderEntity, newItem: ReminderEntity
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun removeItem(reminder: ReminderEntity) {
        val updatedList = currentList.toMutableList().apply { remove(reminder) }
        submitList(updatedList)
    }

}

