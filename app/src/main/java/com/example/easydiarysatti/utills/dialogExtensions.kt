package com.example.easydiarysatti.utills

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.DialogBackgroundBinding
import com.example.easydiarysatti.databinding.DialogImageviewBinding
import com.example.easydiarysatti.databinding.EditFeelingsDialogBinding
import com.example.easydiarysatti.databinding.EditTagDialogBinding
import com.example.easydiarysatti.databinding.EditTextDialogBinding
import com.example.easydiarysatti.databinding.FeedbackLayoutBinding
import com.example.easydiarysatti.databinding.PickPhotoDialogBinding
import com.example.easydiarysatti.domain.model.EmojiInfo
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.saveBitmapToUri
import com.example.easydiarysatti.setExclusiveSelection
import com.example.easydiarysatti.setExclusiveSelectionColor
import com.example.easydiarysatti.setExclusiveSelectionHeadingSize
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

// ─────────────────────────────────────────────────────────────────────────────
//  Feelings / Emotion picker
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.showEditFeelingsDialog(
    sessionManagerRepo: SessionManagerRepo,
    crossinline selectedEmotion: (EmojiInfo) -> Unit
) {
    val binding     = EditFeelingsDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor  = getCurrentThemeColor(sessionManagerRepo)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val dm      = context.resources.displayMetrics
            val hMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            params.width  = dm.widthPixels - 2 * hMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes    = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }

    binding.apply {
        ivClose.backgroundTintList = ColorStateList.valueOf(themeColor)
        tvOnGoingItemLabel1.setTextColor(ColorStateList.valueOf(themeColor))

        val emojiMap = mapOf(
            ivEmojiHappy    to EmojiInfo(R.drawable.angry,    "#42ABD0", "Angry",    "#9870E2"),
            ivEmojiCalm     to EmojiInfo(R.drawable.anxious,  "#5EE3A9", "Anxious",  "#981B9C"),
            ivEmojiSad      to EmojiInfo(R.drawable.calm,     "#FFDE8B", "Calm",     "#848D9B"),
            ivEmojiExcited  to EmojiInfo(R.drawable.events,   "#FF8D95", "Event",    "#F8B903"),
            ivEmojiAngry    to EmojiInfo(R.drawable.family,   "#FFAC81", "Family",   "#475569"),
            ivEmojiPlayful  to EmojiInfo(R.drawable.happy,    "#A29DFB", "Excited",  "#0DF21B"),
            ivEmojiPlayful1 to EmojiInfo(R.drawable.sad,      "#A29DFB", "Sad",      "#0DF21B"),
            ivEmojiPlayful2 to EmojiInfo(R.drawable.personal, "#A29DFB", "Personal", "#0DF21B"),
            ivEmojiPlayful3 to EmojiInfo(R.drawable.travel,   "#A29DFB", "Travel",   "#0DF21B"),
            ivEmojiPlayful4 to EmojiInfo(R.drawable.work,     "#A29DFB", "Work",     "#0DF21B")
        )

        emojiMap.forEach { (view, info) ->
            view.setOnClickListener {
                selectedEmotion(info)
                imageDialog.dismiss()
            }
        }
        ivClose.setOnClickListener { imageDialog.dismiss() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Image crop dialog
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.showImageCropDialog(
    imagePath: String,
    crossinline btnDone: (Uri?) -> Unit,
    crossinline closeDialog: () -> Unit
) {
    val binding     = DialogImageviewBinding.inflate(LayoutInflater.from(this.context ?: return))
    val imageDialog = Dialog(this.context ?: return)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        this.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val params = WindowManager.LayoutParams()
        params.copyFrom(window?.attributes)
        val dm = context.resources.displayMetrics
        params.width  = dm.widthPixels  - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,  50f, dm).toInt()
        params.height = dm.heightPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, dm).toInt()
        window?.attributes = params
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        cropImageView.post { cropImageView.setImagePath(imagePath) }
        binding.imagePath = imagePath
        btnSave.setOnClickListener {
            val bitmap  = cropImageView.getCroppedImage()
            val cropUri = saveBitmapToUri(
                bitmap  = bitmap  ?: return@setOnClickListener,
                context = context ?: return@setOnClickListener
            )
            imageDialog.dismiss()
            btnDone.invoke(cropUri)
        }
        ivDelete.setOnClickListener {
            imageDialog.dismiss()
            closeDialog.invoke()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Background picker dialog
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.showBackgroundDialog(
    sessionManagerRepo: SessionManagerRepo,
    adapterMultiImageAdapter: MultiImageAdapter,
    crossinline closeDialog: () -> Unit
) {
    val context     = this.context ?: return
    val binding     = DialogBackgroundBinding.inflate(LayoutInflater.from(context))
    val imageDialog = Dialog(context)
    val themeColor  = getCurrentThemeColor(sessionManagerRepo)

    imageDialog.apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val dm = context.resources.displayMetrics
            setLayout(
                dm.widthPixels  - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,  50f, dm).toInt(),
                dm.heightPixels - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300f, dm).toInt()
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    adapterMultiImageAdapter.onItemClick = {
        imageDialog.dismiss()
        closeDialog.invoke()
    }
    adapterMultiImageAdapter.onUploadClickIntercept = {
        imageDialog.dismiss()
        closeDialog.invoke()
    }

    binding.apply {
        ivClose.backgroundTintList = ColorStateList.valueOf(themeColor)
        tvOnGoingItemLabel1.setTextColor(ColorStateList.valueOf(themeColor))
        rvBackground.apply {
            adapter = adapterMultiImageAdapter
            setHasFixedSize(true)
        }
        ivClose.setOnClickListener {
            imageDialog.dismiss()
            closeDialog.invoke()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Edit text / font / alignment dialog
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.showEditTexDialog(
    sessionManagerRepo: SessionManagerRepo,
    colorPalette: List<Int>,
    crossinline closeDialog: () -> Unit,
    crossinline fontSelectionListener: (fontName: String) -> Unit,
    crossinline textAlignmentListener: (alignment: String) -> Unit,
    crossinline textBoldListener: (fontSize: Int) -> Unit,
    crossinline textColorListener: (color: Int) -> Unit,
) {
    val binding     = EditTextDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor  = getCurrentThemeColor(sessionManagerRepo)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val dm      = context.resources.displayMetrics
            val hMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._15sdp)
            params.width  = dm.widthPixels - 2 * hMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes    = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }

    binding.apply {
        ivClose.backgroundTintList = ColorStateList.valueOf(themeColor)
        tvOnGoingItemLabel1.setTextColor(ColorStateList.valueOf(themeColor))

        setExclusiveSelection(
            fontIntaliana, fontLeckerli, fontMargarine,
            fontLobster, fontRethink, fontPacifico
        ) { selectedView ->
            val fontName = selectedView.tag as? String
            fontSelectionListener.invoke(fontName.orEmpty())
            Log.d("FontSelected", "Selected font: $fontName")
        }

        setExclusiveSelection(icStartLine, icCenterLine, icEndLine) { selectedView ->
            when (selectedView.id) {
                R.id.icStartLine  -> textAlignmentListener.invoke("left")
                R.id.icCenterLine -> textAlignmentListener.invoke("center")
                R.id.icEndLine    -> textAlignmentListener.invoke("right")
            }
        }

        setExclusiveSelectionHeadingSize(
            icHeadingOne, icHeadingTwo, icHeadingThree
        ) { selectedView ->
            when (selectedView.id) {
                R.id.icHeadingOne   -> textBoldListener.invoke(0)
                R.id.icHeadingTwo   -> textBoldListener.invoke(1)
                R.id.icHeadingThree -> textBoldListener.invoke(2)
            }
        }

        setExclusiveSelectionColor(
            selectedTint   = ContextCompat.getColor(requireContext(), R.color.app_primary_color),
            unselectedTint = ContextCompat.getColor(requireContext(), R.color.track_color),
            views  = arrayOf(icBlackColor, icDarkGrayColor, icLightGrayColor,
                icPinkColor, icGreenishColor, icPurpleColor),
            colors = colorPalette
        ) { _, selectedColor ->
            textColorListener(selectedColor)
        }

        ivClose.setOnClickListener {
            imageDialog.dismiss()
            closeDialog.invoke()
        }
        imageDialog.setOnDismissListener { closeDialog.invoke() }

        icBlueColor.setOnClickListener {
            ColorPickerDialog.Builder(requireContext())
                .setTitle(getString(R.string.colorpicker))
                .setPreferenceName(getString(R.string.mycolorpickerdialog))
                .setPositiveButton(
                    getString(R.string.confirm),
                    ColorEnvelopeListener { envelope, _ ->
                        textColorListener.invoke(envelope.color)
                        imageDialog.dismiss()
                    })
                .setNegativeButton(getString(R.string.cancel)) { d, _ -> d.dismiss() }
                .attachAlphaSlideBar(true)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .show()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feedback / rating dialog
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.showFeedBackDialog(
    crossinline selectedEmotion: (EmojiInfo) -> Unit
) {
    val binding     = FeedbackLayoutBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val dm      = context.resources.displayMetrics
            val hMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._15sdp)
            params.width  = dm.widthPixels - 2 * hMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes    = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        btnCancel.setOnClickListener { imageDialog.dismiss() }
        btnDone.setOnClickListener {
            val rating = ratingBar.rating
            if (rating > 3) navigateToPlayStore() else sendEmailFeedback()
            imageDialog.dismiss()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Edit tag dialog
//
//  Layout IDs (edit_tag_dialog.xml):
//    • tvOnGoingItemLabel1  — "Edit Tags" title
//    • viewHorizontalLine   — accent divider below title
//    • editTags             — input EditText (maxLength 15)
//    • btnCancel            — outlined dismiss button
//    • btnDone              — filled confirm button
//
//  Theme is applied to all four accent elements above.
//
//  Validation order:
//    1. Empty input          → Toast (please_enter_valid_text)
//    2. Name unchanged       → silent dismiss
//    3. Duplicate tag name   → Toast (tag_already_exists)
//    4. Valid, unique name   → onUpdateTag called, dialog dismissed
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.editTagDialog(
    sessionManagerRepo: SessionManagerRepo,
    oldTags: List<CustomTagEntity>,
    selectedTag: CustomTagEntity,
    crossinline onUpdateTag: (List<CustomTagEntity>) -> Unit
) {
    val binding     = EditTagDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor  = getCurrentThemeColor(sessionManagerRepo)

    // ── Window setup ───────────────────────────────────────────────────────────
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val dm      = context.resources.displayMetrics
            val hMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)
            params.width  = dm.widthPixels - 2 * hMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes    = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {

        // ── Theme colour ───────────────────────────────────────────────────────
        //
        //  Every accent element in the dialog gets the resolved theme colour so
        //  it matches whatever the user has set in Color Theme settings.
        //
        tvOnGoingItemLabel1.setTextColor(themeColor)
        viewHorizontalLine.setBackgroundColor(themeColor)
        btnDone.backgroundTintList = ColorStateList.valueOf(themeColor)
        btnCancel.apply {
            setTextColor(themeColor)
            strokeColor = ColorStateList.valueOf(themeColor)
            rippleColor = ColorStateList.valueOf(themeColor)
        }

        // ── Pre-fill input + cursor at end ─────────────────────────────────────
        val originalName = selectedTag.tagName.orEmpty()
        editTags.setText(originalName)
        editTags.setSelection(originalName.length)

        // ── Enable Done only while field is non-empty ──────────────────────────
        btnDone.isEnabled = originalName.isNotBlank()
        editTags.doAfterTextChanged { text ->
            btnDone.isEnabled = !text.isNullOrBlank()
        }

        // ── Cancel ─────────────────────────────────────────────────────────────
        btnCancel.setOnClickListener { imageDialog.dismiss() }

        // ── Confirm / Save ─────────────────────────────────────────────────────
        btnDone.setOnClickListener {
            val newName = editTags.text.toString().trim()

            when {

                // 1. Empty
                newName.isEmpty() -> {
                    Toast.makeText(
                        context,
                        context?.getString(R.string.please_enter_valid_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 2. Same as original — nothing changed, just close
                newName.equals(originalName, ignoreCase = true) -> {
                    imageDialog.dismiss()
                }

                // 3. Duplicate of a different tag in the list
                oldTags.any {
                    it.tagName.orEmpty().equals(newName, ignoreCase = true) &&
                            it.tagName != originalName
                } -> {
                    Toast.makeText(
                        context,
                        context?.getString(R.string.tag_already_exists),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 4. Valid, unique — persist and close
                else -> {
                    val updatedTag  = selectedTag.copy(tagName = newName)
                    val updatedList = oldTags.map { tag ->
                        if (tag.tagName == originalName) updatedTag else tag
                    }
                    onUpdateTag.invoke(updatedList)
                    imageDialog.dismiss()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Photo picker dialog  (camera vs gallery)
// ─────────────────────────────────────────────────────────────────────────────

inline fun Fragment.pickPhotDialog(
    sessionManagerRepo: SessionManagerRepo,
    crossinline cameraCallBack: (Boolean) -> Unit,
    crossinline galleryCallBack: (Boolean) -> Unit,
    crossinline onDismiss: () -> Unit,
) {
    val binding     = PickPhotoDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor  = getCurrentThemeColor(sessionManagerRepo)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val dm      = context.resources.displayMetrics
            val hMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)
            params.width  = dm.widthPixels - 2 * hMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes    = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        ivClose.backgroundTintList    = ColorStateList.valueOf(themeColor)
        icAudio.imageTintList         = ColorStateList.valueOf(themeColor)
        icMusic.imageTintList         = ColorStateList.valueOf(themeColor)
        tvOnGoingItemLabel1.setTextColor(ColorStateList.valueOf(themeColor))

        ivClose.setOnClickListener {
            imageDialog.dismiss()
            onDismiss.invoke()
        }
        viewCamera.setOnClickListener {
            imageDialog.dismiss()
            cameraCallBack.invoke(true)
        }
        viewGallery.setOnClickListener {
            imageDialog.dismiss()
            galleryCallBack.invoke(true)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Play-store / email helpers
// ─────────────────────────────────────────────────────────────────────────────

fun Fragment.navigateToPlayStore() {
    val pkg = requireContext().packageName
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (e: Exception) {
        startActivity(
            Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
        )
    }
}

fun Fragment.sendEmailFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL,   arrayOf("cisco7865@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
        putExtra(Intent.EXTRA_TEXT,    "")
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Theme colour resolver  (shared by every dialog above)
// ─────────────────────────────────────────────────────────────────────────────

fun Fragment.getCurrentThemeColor(sessionManagerRepo: SessionManagerRepo): Int {
    return when (sessionManagerRepo.getBgTheme()) {
        R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
        R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
        R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
        R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
        R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
        else               -> ContextCompat.getColor(requireContext(), R.color.app_primary_color)
    }
}