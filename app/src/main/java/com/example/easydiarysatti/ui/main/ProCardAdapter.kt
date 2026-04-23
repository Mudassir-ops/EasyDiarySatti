package com.example.easydiarysatti.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.R

/**
 * ViewPager2 adapter for the 2-card pro slider in the drawer.
 *
 * Card 0 — "Remove Ads"      (background: remove_ad_pro_card_bg)
 * Card 1 — "Unlock Premium"  (background: premium_pro_card_bg)
 *
 * Each card uses item_pro_card.xml.
 */
class ProCardAdapter(
    private val onRemoveAdsClick: () -> Unit,
    private val onSubscribeClick: () -> Unit
) : RecyclerView.Adapter<ProCardAdapter.ProCardVH>() {

    data class ProCardData(
        val title: String,
        val titleHighlight: String?,
        val subtitle: String,
        val buttonText: String,
        val imageRes: Int,
        val backgroundRes: Int
    )

    private val cards = listOf(
        ProCardData(
            title           = "Remove ",
            titleHighlight  = "Ads",
            subtitle        = "Eliminate annoying ads for smoother\ndistraction-free surfing",
            buttonText      = "Remove Ads",
            imageRes        = R.drawable.remove_ad_ic,
            backgroundRes   = R.drawable.remove_ad_pro_card_bg
        ),
        ProCardData(
            title           = "Unlock ",
            titleHighlight  = "Premium",
            subtitle        = "Unlock Everything! Get the Best\nwith Pro",
            buttonText      = "Subscribe Now",
            imageRes        = R.drawable.unlock_premium_ic,
            backgroundRes   = R.drawable.premium_pro_card_bg
        )
    )

    override fun getItemCount() = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProCardVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pro_card, parent, false)
        return ProCardVH(view)
    }

    override fun onBindViewHolder(holder: ProCardVH, position: Int) {
        holder.bind(cards[position], position)
    }

    inner class ProCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root        = itemView.findViewById<View>(R.id.cardRoot)
        private val tvTitle     = itemView.findViewById<TextView>(R.id.tvCardTitle)
        private val tvHighlight = itemView.findViewById<TextView>(R.id.tvCardTitleHighlight)
        private val tvSubtitle  = itemView.findViewById<TextView>(R.id.tvCardSubtitle)
        private val btnAction   = itemView.findViewById<AppCompatButton>(R.id.btnCardAction)
        private val ivCard      = itemView.findViewById<android.widget.ImageView>(R.id.ivCardImage)

        fun bind(data: ProCardData, position: Int) {
            root.setBackgroundResource(data.backgroundRes)
            tvTitle.text     = data.title
            tvHighlight.text = data.titleHighlight ?: ""
            tvSubtitle.text  = data.subtitle
            btnAction.text   = data.buttonText
            ivCard.setImageResource(data.imageRes)

            // FIX: Use position instead of title-string matching.
            // Old code: data.title.contains("Remove") — breaks if title text ever changes.
            // Position 0 = Remove Ads card, Position 1 = Subscribe/Premium card.
            val clickListener = View.OnClickListener {
                if (position == 0) onRemoveAdsClick.invoke()
                else onSubscribeClick.invoke()
            }

            root.setOnClickListener(clickListener)
            btnAction.setOnClickListener(clickListener)
        }
    }
}