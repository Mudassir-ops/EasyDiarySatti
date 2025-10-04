package com.example.easydiarysatti.utills

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.R
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@BindingAdapter("android:text")
fun setText(view: MaterialTextView, text: CharSequence?) {
    view.text = text
}

@BindingAdapter("formattedTime")
fun MaterialTextView.setFormattedTime(timestamp: Long?) {
    timestamp?.let {
        val date = Date(it)
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        text = sdf.format(date)
    } ?: run {
        text = "--:--"
    }
}

@BindingAdapter("showIfHasImages")
fun AppCompatImageView.showIfHasImages(images: List<Any>?) {
    visibility = if (!images.isNullOrEmpty()) View.VISIBLE else View.GONE
}

@BindingAdapter("firstTagText")
fun MaterialTextView.setFirstTagText(tags: List<String>?) {
    text = tags?.firstOrNull() ?: "Personal"
}

@BindingAdapter("android:visibility")
fun setVisibility(view: View, isVisible: Boolean) {
    view.visibility = if (isVisible) View.VISIBLE else View.GONE
}

@BindingAdapter("app:imageUri")
fun loadImage(view: AppCompatImageView, imageUrl: String?) {
    Log.e("loadImage", "loadImage: $imageUrl")
    imageUrl?.let {
        Glide.with(view.context).load(it)
            .into(view)
    } ?: run {
        Glide.with(view.context).load(R.drawable.image_placeholder)
    }
}

@BindingAdapter("app:lottieAnimationFromType")
fun loadLottieAnimationFromType(view: LottieAnimationView, lottieRes: Int?) {
    if (lottieRes != null) {
        view.setAnimation(lottieRes)
        view.playAnimation()
    }
}

@BindingAdapter("tint")
fun AppCompatImageView.setTintColor(@ColorInt color: Int?) {
    color?.let { setColorFilter(it, PorterDuff.Mode.SRC_IN) }
}

@BindingAdapter("imageRs")
fun loadImage(view: AppCompatImageView, image: Int?) {
    image.let {
        Glide.with(view.context)
            .load(it)
            .into(view)
    }
}

@BindingAdapter("changeBg")
fun changeBg(view: AppCompatButton, color: Int?) {
    color?.let {
        view.backgroundTintList = ColorStateList.valueOf(it)
    }
}

@BindingAdapter("bindBgTint")
fun bindBgTint(view: ConstraintLayout, colorResId: Int?) {
    colorResId?.let { resId ->
        val color = ContextCompat.getColor(view.context, resId)
        view.backgroundTintList = ColorStateList.valueOf(color)
    }
}


@BindingAdapter("app:drawableImage")
fun setDrawableImage(view: AppCompatImageView, drawable: Drawable?) {
    Glide.with(view.context).load(drawable).placeholder(R.drawable.image_placeholder)
        .into(view)
}


@BindingAdapter("app:imageUrlNew")
fun loadImageWithUri(view: AppCompatImageView, imageUrl: Uri?) {
    if (imageUrl != null) {
        Glide.with(view.context)
            .load(imageUrl)
            .error(R.drawable.image_placeholder)
            .into(view)
    }
}

@BindingAdapter("app:imageUri")
fun loadImageThumbnail(view: AppCompatImageView, imageUrl: String?) {
    Log.e("loadImage", "loadImage: $imageUrl")
    imageUrl?.let {
        Glide.with(view.context)
            .load(imageUrl)
            .thumbnail(
                Glide.with(view.context)
                    .load(imageUrl)
                    .sizeMultiplier(0.25f)
            )
            .error(R.drawable.image_placeholder)
            .into(view)
    } ?: run {
        Glide.with(view.context).load(R.drawable.image_placeholder)
    }
}

@BindingAdapter("app:imageUriDialog")
fun loadImageDialog(view: AppCompatImageView, imageUrl: String?) {
    Log.e("loadImage", "loadImage: $imageUrl")
    imageUrl?.let {
        Glide.with(view.context)
            .load(imageUrl)
            .error(R.drawable.image_placeholder)
            .into(view)
    } ?: run {
        Glide.with(view.context).load(R.drawable.image_placeholder)
    }
}

