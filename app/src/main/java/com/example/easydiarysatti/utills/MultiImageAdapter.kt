package com.example.easydiarysatti.utills

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.easydiarysatti.R

// Sealed class to hold either a drawable resource or a gallery URI
sealed class BgItem {
    data class DrawableRes(val resId: Int) : BgItem()
    data class GalleryImage(val uri: Uri)  : BgItem()
}

class MultiImageAdapter(
    private val items: MutableList<BgItem?>,
    private val onUploadClick: () -> Unit,
    private val onImageClick: (source: BgItem?, isPremium: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Grid has 3 columns. Position 0 = upload button.
    // Real background index = adapter position - 1
    //
    // Row 1: pos 0(upload), pos 1(bg0), pos 2(bg1), pos 3(bg2) ← bg2 = 1st row 3rd item
    // Row 2: pos 4(bg3),    pos 5(bg4), pos 6(bg5)             ← bg3 = 2nd row 1st item
    // Row 3: pos 7(bg6),    pos 8(bg7), pos 9(bg8)             ← bg7 = 3rd row 2nd item
    //
    // bgIndex (position - 1):  2, 3, 7  → those are premium
    private val premiumBgIndices = setOf(1, 2, 6)

    /**
     * Set to true once the user has purchased Remove Ads or Premium.
     * When true, [ImageViewHolder.bind] hides the lock/scrim overlay on every
     * premium background thumbnail so purchased users see no gate icons.
     *
     * Always set this BEFORE the dialog opens (MainFragment reads sharedPref at
     * lazy-init time), or call [notifyDataSetChanged] after updating it if the
     * dialog is already visible.
     */
    var isPurchased: Boolean = false

    var onItemClick: ((Int) -> Unit)? = null

    // Called by showBackgroundDialog to dismiss itself when gallery opens
    var onUploadClickIntercept: (() -> Unit)? = null

    // Holds the user-picked gallery URI (null = none picked yet)
    private var galleryUri: Uri? = null

    companion object {
        private const val TYPE_UPLOAD = 0
        private const val TYPE_IMAGE  = 1
    }

    /**
     * Call this from Activity/Fragment after the user picks an image from gallery.
     * Position 0 always stays TYPE_UPLOAD — no ViewHolder type switch, no RecyclerView
     * recycling bug. The upload slot just redraws with the picked image preview behind
     * the upload icon, which always remains tappable to re-open the picker.
     */
    fun setGalleryImage(uri: Uri) {
        galleryUri = uri
        notifyItemChanged(0)
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_UPLOAD else TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_UPLOAD -> UploadViewHolder(
                inflater.inflate(R.layout.bg_item_pick_layout, parent, false)
            )
            else -> ImageViewHolder(
                inflater.inflate(R.layout.bg_item_layout, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UploadViewHolder -> holder.bind(
                galleryUri     = galleryUri,
                onUploadClick  = onUploadClick,
                onApplyGallery = {
                    // Tap on preview → apply the already-picked gallery image
                    galleryUri?.let { onImageClick(BgItem.GalleryImage(it), false) }
                }
            )
            is ImageViewHolder -> {
                val bgIndex   = position - 1
                val isPremium = bgIndex in premiumBgIndices
                holder.bind(items[position], isPremium, isPurchased, onImageClick)
            }
        }
    }

    // ── Upload Button ViewHolder ──────────────────────────────────────────────
    // Two states:
    // 1. No image picked yet  → upload icon only, tap opens gallery
    // 2. Image already picked → gallery preview behind upload icon
    //      • Tap upload icon  → opens gallery to CHANGE the image
    //      • Tap preview area → APPLIES the already-picked image
    //
    // Add this to bg_item_pick_layout XML (BEHIND iv_upload):
    //   <ImageView
    //       android:id="@+id/iv_gallery_preview"
    //       android:layout_width="match_parent"
    //       android:layout_height="match_parent"
    //       android:scaleType="centerCrop"
    //       android:visibility="gone" />
    inner class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val uploadView  = itemView.findViewById<View>(R.id.iv_upload)


        fun bind(
            galleryUri: Uri?,
            onUploadClick: () -> Unit,
            onApplyGallery: () -> Unit
        ) {
            // Always show upload icon only — no preview thumbnail ever

            itemView.setOnClickListener   { onUploadClick()
            }
            uploadView.setOnClickListener { onUploadClick() }
        }
    }

    // ── Background Image ViewHolder ───────────────────────────────────────────
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView    = itemView.findViewById<AppCompatImageView>(R.id.iv_bg)
        private val premiumLock  = itemView.findViewById<View>(R.id.iv_premium_lock)
        private val premiumScrim = itemView.findViewById<View>(R.id.v_premium_scrim)

        fun bind(
            item: BgItem?,
            isPremium: Boolean,
            isPurchased: Boolean,
            onImageClick: (source: BgItem?, isPremium: Boolean) -> Unit
        ) {
            val loadTarget: Any? = when (item) {
                is BgItem.DrawableRes  -> item.resId
                is BgItem.GalleryImage -> item.uri
                null                   -> null
            }

            Glide.with(itemView.context)
                .load(loadTarget)
                .transform(RoundedCorners(24))
                .into(imageView)

            // Show lock/scrim only when the slot is premium AND user has NOT purchased.
            // Once isPurchased = true the overlay is hidden for all thumbnails.
            val overlayVisibility = if (isPremium && !isPurchased) View.VISIBLE else View.GONE
            premiumLock?.visibility  = overlayVisibility
            premiumScrim?.visibility = overlayVisibility

            imageView.setOnClickListener {
                onImageClick(item, isPremium)
                onItemClick?.invoke(bindingAdapterPosition)
            }
        }
    }
}