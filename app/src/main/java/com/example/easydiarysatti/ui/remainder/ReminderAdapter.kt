package com.example.easydiarysatti.ui.remainder

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.ItemReminderBinding


class ReminderAdapter(
    private var list: List<CreateNoteEntity>,
    private val context: Context,
    private val onItemClick: (CreateNoteEntity) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataModel = list[position]
        // holder.binding.txtReminderTime.text = dataModel.remainderTime
        holder.binding.icCross.setOnClickListener {
            onItemClick.invoke(dataModel)
        }

    }

    class ViewHolder(val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("NotifyDataSetChanged")
    fun updateReminderList(newReminderList: List<CreateNoteEntity>) {
        list = newReminderList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeItem(reminder: CreateNoteEntity) {
        val updatedList = list.toMutableList()
        updatedList.remove(reminder)
        list = updatedList
        notifyDataSetChanged()
    }

}
