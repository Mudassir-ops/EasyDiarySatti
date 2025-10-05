package com.example.easydiarysatti


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


fun NavController.safeNav(
    @IdRes currentDestId: Int,
    @IdRes actionId: Int,
    bundle: Bundle? = null
) {
    if (currentDestination?.id == currentDestId) {
        navigate(actionId, bundle)
    }
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}


fun saveUriToCache(context: Context, uri: Uri): File? {
    return try {
        val fileName = "${System.currentTimeMillis()}.jpg" // or get from uri
        val cacheFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(cacheFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        cacheFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun View?.showDatePicker(onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    this?.context?.let {
        DatePickerDialog(
            it,
            { _, year, month, dayOfMonth ->
                val dateString = "$dayOfMonth/${month + 1}/$year"
                onDateSelected(dateString)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }?.show()
}

fun View.setCustomRipple(
    rippleColor: Int,
    onClick: () -> Unit
) {
    // 1) Create the ripple drawable
    val colorStateList = ColorStateList.valueOf(rippleColor)
    val rippleDrawable = RippleDrawable(
        colorStateList,
        null,
        null
    )
    // 2) Apply it
    this.foreground = rippleDrawable
    this.isClickable = true

    // 3) Set click
    setOnClickListener { onClick() }
}

fun Context?.monthlyFormatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun Activity?.showExitDialog() {
    this?.let { ctx ->
        AlertDialog.Builder(ctx)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setCancelable(true)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                finish() // exit the app
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

private var currentToast: Toast? = null

fun showToast(context: Context, message: String) {
    currentToast?.cancel()
    currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
    currentToast?.show()
}

//fun RecyclerView.showShimmer(count: Int = 5) {
//    adapter = ShimmerAdapter(count)
//}

fun <T, VH : RecyclerView.ViewHolder> RecyclerView.showData(
    adapter: RecyclerView.Adapter<VH>,
    data: List<T>,
    submit: (RecyclerView.Adapter<VH>, List<T>) -> Unit
) {
    this.adapter = adapter
    submit(adapter, data)
}

fun AppCompatButton.updateButtonState(
    isEnabled: Boolean,
    backgroundColor: ColorStateList?,
    textColor: ColorStateList? = null
) {
    this.isClickable = isEnabled
    this.isEnabled = isEnabled
    this.backgroundTintList = backgroundColor
    textColor?.let { this.setTextColor(it) }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.changeBgTintList(@ColorRes color: Int) {
    this.backgroundTintList =
        ContextCompat.getColorStateList(this.context, color)
}

fun updateButtonSelection(
    selected: View,
    others: List<View>
) {
    selected.changeBgTintList(R.color.app_color)
    others.forEach { it.changeBgTintList(R.color.black) }
}

fun Context.hasPermissions(permissions: List<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(
            this,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun Fragment.hasPermissions(permissions: List<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(
            requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun Fragment.validateStoragePermission(): Boolean {
    val permissions = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return hasPermissions(permissions = permissions)
}

fun View.setSafeClickListener(delayMillis: Long = 1000L, onClick: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= delayMillis) {
            lastClickTime = currentTime
            onClick(v)
        }
    }
}

fun View.setSelectedBg(isSelected: Boolean) {
    val drawableRes =
        if (isSelected) R.drawable.language_item_selected else R.drawable.language_item_bg
    this.setBackgroundResource(drawableRes)
}

fun View.showSnackbar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    anchor: View? = null
) {
    Snackbar.make(this, message, duration).apply {
        anchor?.let { anchorView = it }
    }.show()
}

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    requireView().let { v ->
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}

fun AppCompatImageView.loadImage(
    resourceId: Int?,
    placeholder: Int = R.drawable.image_placeholder
) {
    Glide.with(this.context)
        .load(resourceId ?: placeholder)
        .into(this)
}


fun AppCompatImageView.loadAdaptiveImage(
    imagePath: String?,
    placeholder: Int = R.drawable.image_placeholder,
) {
    Glide.with(this.context)
        .load(imagePath)
        .thumbnail(0.1f)
        .centerCrop()
        .into(this)

//    Glide.with(this.context)
//        .asBitmap()
//        .load(imagePath ?: placeholder)
//        .placeholder(placeholder)
//        .error(placeholder)
//        .into(object : CustomTarget<Bitmap>() {
//            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                val targetWidth = width
//                val targetHeight = height
//
//                if (targetWidth <= 0 || targetHeight <= 0) {
//                    // View not measured yet; reload later
//                    post { loadAdaptiveImage(imagePath, placeholder) }
//                    return
//                }
//
//                val resizedBitmap = resizeToFitView(resource, targetWidth, targetHeight)
//                setImageBitmap(resizedBitmap)
//            }
//
//            override fun onLoadCleared(placeholder: Drawable?) {
//                setImageDrawable(placeholder)
//            }
//        })
}

/**
 * Resize the image to fit nicely inside the target view without distortion.
 * - Keeps aspect ratio
 * - Crops slightly if needed for balance
 */
private fun resizeToFitView(
    source: Bitmap,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    val srcWidth = source.width
    val srcHeight = source.height
    val srcRatio = srcWidth.toFloat() / srcHeight
    val targetRatio = targetWidth.toFloat() / targetHeight

    return if (srcRatio > targetRatio) {
        // Source is wider than target → crop width
        val newWidth = (srcHeight * targetRatio).toInt()
        val xOffset = (srcWidth - newWidth) / 2
        Bitmap.createBitmap(source, xOffset, 0, newWidth, srcHeight)
    } else {
        // Source is taller → crop height
        val newHeight = (srcWidth / targetRatio).toInt()
        val yOffset = (srcHeight - newHeight) / 2
        Bitmap.createBitmap(source, 0, yOffset, srcWidth, newHeight)
    }
}


fun View.loadBackground(
    resourceId: Int?,
    placeholder: Int = R.drawable.image_placeholder
) {
    Glide.with(this.context)
        .load(resourceId ?: placeholder)
        .placeholder(placeholder)
        .into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {
            override fun onResourceReady(
                resource: Drawable,
                transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
            ) {
                background = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                background = placeholder
            }
        })
}


fun Fragment.setKeyboardVisibilityListener(onVisibilityChanged: (Boolean) -> Unit) {
    val activity = activity ?: return
    val contentView = activity.findViewById<View>(android.R.id.content)
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        contentView.getWindowVisibleDisplayFrame(r)
        val screenHeight = contentView.rootView.height
        val keypadHeight = screenHeight - r.bottom
        onVisibilityChanged(keypadHeight > screenHeight * 0.15)
    }
    contentView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            contentView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    })
}

fun Fragment.setKeyboardVisibilityListenerCreateNote(onVisibilityChanged: (Boolean) -> Unit) {
    val activity = activity ?: return
    val contentView = activity.findViewById<View>(android.R.id.content)
    val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        contentView.getWindowVisibleDisplayFrame(r)
        val screenHeight = contentView.rootView.height
        val keypadHeight = screenHeight - r.bottom
        onVisibilityChanged(keypadHeight > screenHeight * 0.5)
    }
    contentView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            contentView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    })
}


@Suppress("DEPRECATION")
fun Fragment.enableResize(enable: Boolean) {
    val mode = if (enable) {
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
    } else {
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
    }
    activity?.window?.setSoftInputMode(mode)
}

fun Long.toDateString(): String {
    val sdf = java.text.SimpleDateFormat("d MMMM, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

fun View.setRoundedBgColors(
    @ColorInt solidColor: Int,
    @ColorInt strokeColor: Int,
    strokeWidth: Int = 2
) {
    (background as? GradientDrawable)?.mutate()?.let { it as GradientDrawable }?.apply {
        setColor(solidColor)
        setStroke(strokeWidth, strokeColor)
    }
}

fun TextView.setVectorDrawable(
    drawableRes: Int,
    sizeInPx: Int,
    position: DrawablePosition = DrawablePosition.START
) {
    val drawable = ContextCompat.getDrawable(context, drawableRes)?.apply {
        setBounds(0, 0, sizeInPx, sizeInPx)
    }

    when (position) {
        DrawablePosition.START -> setCompoundDrawables(drawable, null, null, null)
        DrawablePosition.TOP -> setCompoundDrawables(null, drawable, null, null)
        DrawablePosition.END -> setCompoundDrawables(null, null, drawable, null)
        DrawablePosition.BOTTOM -> setCompoundDrawables(null, null, null, drawable)
    }
}

enum class DrawablePosition { START, TOP, END, BOTTOM }


fun Int.dpToPx(context: Context): Int =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        context.resources.displayMetrics
    ).toInt()


fun FlexboxLayout.addTags(
    tagList: MutableList<String>,
    onTagClick: ((String) -> Unit)? = null,
    onRemoveTagClick: ((String) -> Unit)? = null,
) {
    if (tagList.isEmpty()) {
        this.visibility = View.GONE
        return
    } else {
        this.visibility = View.VISIBLE
    }
    this.removeAllViews()
    for (tag in tagList) {
        val tagContainer = LinearLayout(this.context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            setBackgroundResource(R.drawable.bg_tag)
            layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
                bottomMargin = 8
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val hashIcon = ImageView(this.context).apply {
            setImageResource(R.drawable.ic_hash_small)
            imageTintList = ContextCompat.getColorStateList(context, R.color.tag_txt_color)
            setPadding(8, 8, 4, 8)
        }

        val tagText = TextView(this.context).apply {
            text = tag
            setPadding(0, 8, 8, 8)
            setTextColor(ContextCompat.getColor(context, R.color.tag_txt_color))
            textSize = 14f
            //  typeface = ResourcesCompat.getFont(context, R.font.outfit_medium)
            setOnClickListener {
                onTagClick?.invoke(tag)
            }
        }

        val closeIcon = ImageView(this.context).apply {
            setImageResource(R.drawable.ic_baseline_close_16)
            setPadding(8, 12, 12, 8)
            imageTintList = ContextCompat.getColorStateList(context, R.color.tag_txt_color)
            setOnClickListener {
                val ifOnlyUnknown = tagList.any { it == "Personal" }
                if (ifOnlyUnknown) {
                    onTagClick?.invoke(tag)
                    return@setOnClickListener
                }
                this@addTags.removeView(tagContainer)
                tagList.remove(tag)
                onRemoveTagClick?.invoke(tag)
                if (tagList.isEmpty()) {
                    this@addTags.visibility = View.GONE
                }
            }
        }

        tagContainer.addView(hashIcon)
        tagContainer.addView(tagText)
        tagContainer.addView(closeIcon)

        this.addView(tagContainer)
    }
}

fun FragmentHomeBinding?.visible(hasNotes: Boolean) {
    this?.apply {
        rvNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE
        noNotesLayout.visibility = if (hasNotes) View.GONE else View.VISIBLE
    }
}

fun showPermissionDialog(context: Context, fragment: Fragment) {
    val builder = android.app.AlertDialog.Builder(fragment.requireActivity())
    val dialog = builder.setTitle("Permission Required")
        .setMessage("Required permissions have been set to 'Don't ask again'. Please enable them in settings.")
        .setCancelable(true)
        .setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        .setPositiveButton("Settings") { dialogInterface, _ ->
            redirectToSystemSettings(context = context)
            dialogInterface.dismiss()
        }
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            redirectToSystemSettings(context)
            dialog.dismiss()
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            dialog.dismiss()
        }
    }
    dialog.show()
}

private fun redirectToSystemSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

fun saveBitmapToUri(bitmap: Bitmap, context: Context): Uri? {
    val imageFile =
        File(context.cacheDir, "img_" + Calendar.getInstance().timeInMillis + ".jpg")
    Log.e("cropFragment", "saveBitmapToUri: $imageFile")
    try {
        val fos = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.close()
        return Uri.fromFile(imageFile)
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun DayOfWeek.getShortDisplayNameCompat(locale: Locale = Locale.getDefault()): String {
    val shortWeekdays = DateFormatSymbols(locale).shortWeekdays
    val dayIndex = if (this == DayOfWeek.SUNDAY) 1 else this.value + 1
    return shortWeekdays[dayIndex]
}


fun getDayRangeMillis(localDate: LocalDate): Pair<Long, Long> {
    val zoneId = ZoneId.systemDefault()
    val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endOfDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
    return startOfDay to endOfDay
}

fun dateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.getDefault())

fun lightenColor(color: Int, factor: Float): Int {
    val r = ((Color.red(color) * (1 - factor) / 255 + factor) * 255).toInt().coerceIn(0, 255)
    val g = ((Color.green(color) * (1 - factor) / 255 + factor) * 255).toInt().coerceIn(0, 255)
    val b = ((Color.blue(color) * (1 - factor) / 255 + factor) * 255).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, b)
}

fun AppCompatImageView.setCustomDayEmojiBackground(
    fillColor: String?,
    strokeColor: String?,
    dayNow: Boolean = false
) {
    val drawable = ContextCompat.getDrawable(context, R.drawable.bg_note_item)?.mutate()
    if (drawable is GradientDrawable) {
        fillColor?.let {
            drawable.setColor(lightenColor(it.toColorInt(), 0.65f))
        }
        strokeColor?.let {
            val strokeWidth = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._1sdp)
            drawable.setStroke(strokeWidth, it.toColorInt())
        }
    }
    if (dayNow) {
        setColorFilter(
            ContextCompat.getColor(context, R.color.red_color),
            PorterDuff.Mode.SRC_IN
        )
    } else {
        setColorFilter(
            ContextCompat.getColor(context, R.color.tag_txt_color),
            PorterDuff.Mode.SRC_IN
        )
    }
    background = drawable
}

fun Context.dpToPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).toInt()
