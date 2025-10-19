package com.example.easydiarysatti.ui.remainder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.databinding.ItemReminderBinding
import com.example.easydiarysatti.databinding.ItemReminderNoteBinding

class ReminderAdapter(
    private val onItemClick: (ReminderEntity) -> Unit
) : ListAdapter<ReminderEntity, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_DAILY = 0
        private const val VIEW_TYPE_NOTE = 1

        private val DiffCallback = object : DiffUtil.ItemCallback<ReminderEntity>() {
            override fun areItemsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ReminderEntity, newItem: ReminderEntity) =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).noteReminder) VIEW_TYPE_NOTE else VIEW_TYPE_DAILY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NOTE -> {
                val binding = ItemReminderNoteBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                NoteReminderViewHolder(binding)
            }

            else -> {
                val binding = ItemReminderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                DailyReminderViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is NoteReminderViewHolder -> holder.bind(item)
            is DailyReminderViewHolder -> holder.bind(item)
        }
    }

    inner class DailyReminderViewHolder(private val binding: ItemReminderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReminderEntity) {
            binding.txtReminderTime.text = item.formattedDate
            binding.icCross.setOnClickListener { onItemClick(item) }
        }
    }

    inner class NoteReminderViewHolder(private val binding: ItemReminderNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReminderEntity) {
            binding.txtReminderTime.text = item.formattedDate
            binding.icCross.setOnClickListener { onItemClick(item) }
        }
    }

    fun removeItem(reminder: ReminderEntity) {
        val updatedList = currentList.toMutableList().apply { remove(reminder) }
        submitList(updatedList)
    }
}


