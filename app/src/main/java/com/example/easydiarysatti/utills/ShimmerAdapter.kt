package com.example.easydiarysatti.utills

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.R

class ShimmerAdapter(private val itemCount: Int) :
    RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder>() {

    class ShimmerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shimmer, parent, false)
        return ShimmerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        // No binding needed for shimmer
    }

    override fun getItemCount(): Int = itemCount
}