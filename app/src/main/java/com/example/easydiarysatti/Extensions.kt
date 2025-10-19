package com.example.easydiarysatti


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
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
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.remainder.AlarmHandler
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
    bundle: Bundle? = null,
    enterAnim: Int = R.anim.slide_in_right,
    exitAnim: Int = R.anim.slide_out_left,
    popEnterAnim: Int = R.anim.slide_in_left,
    popExitAnim: Int = R.anim.slide_out_right
) {
    if (currentDestination?.id == currentDestId) {
        val navOptions = NavOptions.Builder()
            .setEnterAnim(enterAnim)
            .setExitAnim(exitAnim)
            .setPopEnterAnim(popEnterAnim)
            .setPopExitAnim(popExitAnim)
            .build()

        navigate(actionId, bundle, navOptions)
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
        .load(resourceId)
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
    placeholder: Int? = null
) {
    val request = Glide.with(context)
        .load(resourceId)
    placeholder?.let { request.placeholder(it) }
    request.into(object : com.bumptech.glide.request.target.CustomTarget<Drawable>() {
        override fun onResourceReady(
            resource: Drawable,
            transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
        ) {
            background = resource
        }

        override fun onLoadCleared(placeholderDrawable: Drawable?) {
            background = placeholderDrawable
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
    fromPreview: Boolean = false,
    tagList: MutableList<CustomTagEntity>,
    onTagClick: ((CustomTagEntity) -> Unit)? = null,
    onRemoveTagClick: ((CustomTagEntity) -> Unit)? = null,
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
            text = tag.tagName
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
                if (fromPreview) return@setOnClickListener
                val onlyPersonalLeft =
                    tagList.size == 1 && tagList.firstOrNull()?.tagName == "Personal"
                if (onlyPersonalLeft) {
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

fun setStyledDateTime(tvDate: MaterialTextView, colorId: Int) {
    val formatter = SimpleDateFormat("dd-MM-yy | h:mm a", Locale.getDefault())
    val formatted = formatter.format(Date()).uppercase(Locale.getDefault())
    val parts = formatted.split("|")
    val datePart = parts.getOrNull(0)?.trim() ?: ""
    val timePart = parts.getOrNull(1)?.trim() ?: ""

    val separator = " | "

    val spannable = SpannableStringBuilder().apply {
        append(datePart)
        setSpan(
            StyleSpan(Typeface.BOLD),
            0, datePart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        val startSeparator = length
        append(separator)
        setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    tvDate.context ?: return,
                    R.color.app_primary_color
                )
            ),
            startSeparator,
            startSeparator + separator.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val startTime = length
        append(timePart)
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor(tvDate.context, colorId)),
            startTime,
            startTime + timePart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    tvDate.text = spannable
}

fun setStyledDateAlreadyTime(tvDate: MaterialTextView, colorId: Int, formatted: String) {

    val parts = formatted.split("|")
    val datePart = parts.getOrNull(0)?.trim() ?: ""
    val timePart = parts.getOrNull(1)?.trim() ?: ""
    val separator = " | "

    val spannable = SpannableStringBuilder().apply {
        // Date (bold)
        append(datePart)
        setSpan(
            StyleSpan(Typeface.BOLD),
            0, datePart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Separator (app color)
        val separatorColor = ContextCompat.getColor(tvDate.context, R.color.app_primary_color)
        val startSeparator = length
        append(separator)
        setSpan(
            ForegroundColorSpan(separatorColor),
            startSeparator,
            startSeparator + separator.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Time (custom color)
        val timeColor = ContextCompat.getColor(tvDate.context, colorId)
        val startTime = length
        append(timePart)
        setSpan(
            ForegroundColorSpan(timeColor),
            startTime,
            startTime + timePart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    tvDate.text = spannable
}


fun setExclusiveSelection(
    vararg textViews: MaterialTextView,
    onSelected: (selectedView: MaterialTextView) -> Unit
) {
    textViews.forEach { textView ->
        textView.setOnClickListener {
            textViews.forEach {
                it.isSelected = false
                it.setTextColor(ContextCompat.getColor(it.context, R.color.track_color))
            }
            textView.isSelected = true
            textView.setTextColor(
                ContextCompat.getColor(
                    textView.context,
                    R.color.app_primary_color
                )
            )
            onSelected.invoke(textView)
        }
    }
}


fun AppCompatEditText.setFont(fontName: String, context: Context?) {
    val typeface = when (fontName) {
        "Intaliana" -> ResourcesCompat.getFont(context ?: return, R.font.italiana_regular)
        "Leckerli" -> ResourcesCompat.getFont(context ?: return, R.font.leckerlione_regular)
        "Margarine" -> ResourcesCompat.getFont(context ?: return, R.font.margarine_regular)
        "Rethink" -> ResourcesCompat.getFont(context ?: return, R.font.rethinksans_regular)
        "Pacifico" -> ResourcesCompat.getFont(context ?: return, R.font.pacifico)
        "Lobster" -> ResourcesCompat.getFont(context ?: return, R.font.lobster_regular)
        else -> null
    }
    typeface?.let {
        this.typeface = it
    }
}


fun setExclusiveSelection(
    vararg views: AppCompatImageView,
    onSelected: (AppCompatImageView) -> Unit = {}
) {
    views.forEach { view ->
        view.setOnClickListener {
            views.forEach { it.isSelected = false; it.alpha = 0.5f }
            view.isSelected = true
            view.alpha = 1f
            onSelected(view)
        }
    }
}

fun setExclusiveSelectionHeadingSize(
    vararg views: MaterialTextView,
    onSelected: (MaterialTextView) -> Unit = {}
) {
    views.forEach { view ->
        view.setOnClickListener {
            views.forEach { it.isSelected = false; it.alpha = 0.5f }
            view.isSelected = true
            view.alpha = 1f
            onSelected(view)
        }
    }
}

fun setTextAlignmentByName(textView: AppCompatEditText, alignment: String) {
    when (alignment.lowercase()) {
        "left" -> textView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        "center" -> textView.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        "right" -> textView.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        else -> textView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
    }
}

fun AppCompatEditText?.setHeadingSize(textSizeInSp: Float) {
    this?.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeInSp)
}

fun getHeadingSize(index: Int): Float {
    return when (index) {
        0 -> 19f
        1 -> 20f
        2 -> 23f
        else -> 19F
    }
}

fun setExclusiveSelectionColor(
    selectedTint: Int,
    unselectedTint: Int,
    views: Array<AppCompatImageView>,
    colors: List<Int>,
    onSelected: (AppCompatImageView, Int) -> Unit = { _, _ -> }
) {
    views.forEachIndexed { index, view ->
        view.setOnClickListener {
            views.forEach {
                it.isSelected = false
                it.alpha = 0.6f
                it.imageTintList = ColorStateList.valueOf(unselectedTint)
            }
            view.isSelected = true
            view.alpha = 1f
            view.imageTintList = ColorStateList.valueOf(selectedTint)
            val color = colors.getOrNull(index) ?: Color.BLACK
            onSelected(view, color)
        }
    }
}

fun Activity?.isNotificationEnabled(): Boolean {
    val notificationManagerCompat =
        NotificationManagerCompat.from(this@isNotificationEnabled ?: return false)
    return notificationManagerCompat.areNotificationsEnabled()
}

fun Activity?.notificationPermission() {
    if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationManagerCompat =
            NotificationManagerCompat.from(this@notificationPermission ?: return)
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
        }
    }
}

fun Activity?.setReminderEasyDiary(
    calendar: Calendar,
    text: String,
    uniqueId: Int,
    contentTitle: String
) {
    val alarmHandler = AlarmHandler(this@setReminderEasyDiary ?: return)
    alarmHandler.createAlarm(calendar, text, uniqueId = uniqueId, contentTitle = contentTitle)
}


fun Fragment.showDatePickerWithTime(
    calendar: Calendar = Calendar.getInstance(),
    onDateTimeSelected: (Calendar) -> Unit
) {
    val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.TimePickerDialogTheme)

    DatePickerDialog(
        contextThemeWrapper,
        { _, year, monthOfYear, dayOfMonth ->
            calendar.set(year, monthOfYear, dayOfMonth)
            showTimePicker(calendar) {
                onDateTimeSelected(calendar)
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun Fragment.showTimePicker(calendar: Calendar, onTimeSelected: () -> Unit) {
    val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.TimePickerDialogTheme)

    TimePickerDialog(
        contextThemeWrapper,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            onTimeSelected()
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

fun Date.toFormattedString(pattern: String, locale: Locale = Locale.getDefault()): String {
    val dateFormat = SimpleDateFormat(pattern, locale)
    return dateFormat.format(this)
}




