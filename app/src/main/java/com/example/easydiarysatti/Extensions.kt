package com.example.easydiarysatti


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

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
        .placeholder(placeholder)
        .into(this)
}

