package com.example.easydiarysatti.ui.edittags

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.ItemTagsBinding

class TagsAdapter(
    private val onItemClick: (Triple<List<CustomTagEntity>, Int, CustomTagEntity>) -> Unit
) : ListAdapter<CustomTagEntity, TagsAdapter.ViewHolder>(DiffCallback()) {

    private var fullList: List<CustomTagEntity> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTagsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataModel = getItem(position)
        with(holder.binding) {
            txtTagName.text = dataModel.tagName

            icEdit.setOnClickListener {
                onItemClick.invoke(
                    Triple(
                        fullList.filter { it.noteId == dataModel.noteId },
                        EDIT_ACTION,
                        dataModel
                    )
                )
            }

            icDelete.setOnClickListener {
                onItemClick.invoke(
                    Triple(
                        fullList.filter { it.noteId == dataModel.noteId },
                        DELETE_ACTION,
                        dataModel
                    )
                )
            }


            itemView.setOnClickListener {
                onItemClick.invoke(
                    Triple(
                        fullList.filter { it.noteId == dataModel.noteId },
                        ITEM_CLICK,
                        dataModel
                    )
                )
            }
        }
    }

    class ViewHolder(val binding: ItemTagsBinding) : RecyclerView.ViewHolder(binding.root)

//    override fun submitList(list: List<CustomTagEntity>?) {
//        fullList = list ?: emptyList()
//        super.submitList(ArrayList(list ?: emptyList()))
//    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.tagName.contains(query, ignoreCase = true)
            }
        }
        super.submitList(ArrayList(filteredList))
    }

    // Inside TagsAdapter.kt
    class DiffCallback : DiffUtil.ItemCallback<CustomTagEntity>() {
        override fun areItemsTheSame(oldItem: CustomTagEntity, newItem: CustomTagEntity): Boolean {
            // Fix: If the name is different, treat it as a different item so it refreshes
            return oldItem.noteId == newItem.noteId && oldItem.tagName == newItem.tagName
        }

        override fun areContentsTheSame(oldItem: CustomTagEntity, newItem: CustomTagEntity): Boolean {
            return oldItem == newItem
        }
    }

    override fun submitList(list: List<CustomTagEntity>?) {
        fullList = list ?: emptyList()
        // Fix: Always pass a NEW ArrayList instance so DiffUtil is forced to check changes
        super.submitList(if (list != null) ArrayList(list) else null)
    }

    companion object {
        const val EDIT_ACTION = 1
        const val DELETE_ACTION = 2
        const val ITEM_CLICK = 3
    }
}






