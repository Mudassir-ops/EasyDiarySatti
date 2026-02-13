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
import javax.inject.Inject

inline fun Fragment.showEditFeelingsDialog(
    sessionManagerRepo: SessionManagerRepo,
    crossinline selectedEmotion: (EmojiInfo) -> Unit
) {
    val binding = EditFeelingsDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor = getCurrentThemeColor(sessionManagerRepo)
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
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
            ivEmojiHappy to EmojiInfo(
                drawableRes = R.drawable.angry,
                colorHex = "#42ABD0",
                name = "Angry",
                tagColor = "#9870E2"
            ), ivEmojiCalm to EmojiInfo(
                drawableRes = R.drawable.anxious,
                colorHex = "#5EE3A9",
                name = "Anxious",
                tagColor = "#981B9C"
            ), ivEmojiSad to EmojiInfo(
                drawableRes = R.drawable.calm,
                colorHex = "#FFDE8B",
                name = "Calm",
                tagColor = "#848D9B"
            ), ivEmojiExcited to EmojiInfo(
                drawableRes = R.drawable.events,
                colorHex = "#FF8D95",
                name = "Event",
                tagColor = "#F8B903"
            ), ivEmojiAngry to EmojiInfo(
                drawableRes = R.drawable.family,
                colorHex = "#FFAC81",
                name = "Family",
                tagColor = "#475569"
            ), ivEmojiPlayful to EmojiInfo(
                drawableRes = R.drawable.happy,
                colorHex = "#A29DFB",
                name = "Excited",
                tagColor = "#0DF21B"
            ), ivEmojiPlayful1 to EmojiInfo(
                drawableRes = R.drawable.sad,
                colorHex = "#A29DFB",
                name = "Sad",
                tagColor = "#0DF21B"
            ), ivEmojiPlayful2 to EmojiInfo(
                drawableRes = R.drawable.personal,
                colorHex = "#A29DFB",
                name = "Personal",
                tagColor = "#0DF21B"
            ), ivEmojiPlayful3 to EmojiInfo(
                drawableRes = R.drawable.travel,
                colorHex = "#A29DFB",
                name = "Travel",
                tagColor = "#0DF21B"
            ), ivEmojiPlayful4 to EmojiInfo(
                drawableRes = R.drawable.work,
                colorHex = "#A29DFB",
                name = "Work",
                tagColor = "#0DF21B"
            )
        )

        emojiMap.forEach { (view, pair) ->
            view.setOnClickListener {
                selectedEmotion(pair)
                imageDialog.dismiss()
            }
        }

        ivClose.setOnClickListener { imageDialog.dismiss() }
    }
}

inline fun Fragment.showImageCropDialog(
    imagePath: String, crossinline btnDone: (Uri?) -> Unit, crossinline closeDialog: () -> Unit
) {
    val binding = DialogImageviewBinding.inflate(LayoutInflater.from(this.context ?: return))
    val imageDialog = Dialog(this.context ?: return)
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)


        this.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT
        )
        val params = WindowManager.LayoutParams()
        params.copyFrom(window?.attributes)
        val displayMetrics = context.resources.displayMetrics
        val marginWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 50f, displayMetrics
        ).toInt()
        params.width = displayMetrics.widthPixels - marginWidthPx
        val marginHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 300f, displayMetrics
        ).toInt()
        params.height = displayMetrics.heightPixels - marginHeightPx
        window?.attributes = params
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        binding.cropImageView.post {
            cropImageView.setImagePath(imagePath)
        }
        binding.imagePath = imagePath
        btnSave.setOnClickListener {
            val bitmap = cropImageView.getCroppedImage()
            val cropUri = saveBitmapToUri(
                bitmap = bitmap ?: return@setOnClickListener,
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

inline fun Fragment.showBackgroundDialog(
    sessionManagerRepo: SessionManagerRepo,
    adapterMultiImageAdapter: MultiImageAdapter, crossinline closeDialog: () -> Unit
) {
    val context = this.context ?: return
    val binding = DialogBackgroundBinding.inflate(LayoutInflater.from(context))
    val imageDialog = Dialog(context)
    val themeColor = getCurrentThemeColor(sessionManagerRepo)
    imageDialog.apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        window?.apply {
            val displayMetrics = context.resources.displayMetrics
            val marginWidthPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50f, displayMetrics
            ).toInt()
            val marginHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 300f, displayMetrics
            ).toInt()
            setLayout(
                displayMetrics.widthPixels - marginWidthPx,
                displayMetrics.heightPixels - marginHeightPx
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

inline fun Fragment.showEditTexDialog(
    sessionManagerRepo: SessionManagerRepo,
    colorPalette: List<Int>,
    crossinline closeDialog: () -> Unit,
    crossinline fontSelectionListener: (fontName: String) -> Unit,
    crossinline textAlignmentListener: (alignment: String) -> Unit,
    crossinline textBoldListener: (fontSize: Int) -> Unit,
    crossinline textColorListener: (color: Int) -> Unit,
) {
    val binding = EditTextDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor = getCurrentThemeColor(sessionManagerRepo)
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._15sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }

    binding.apply {
        /* 🔹 CLOSE ICON */
        ivClose.backgroundTintList = ColorStateList.valueOf(themeColor)
        tvOnGoingItemLabel1.setTextColor(ColorStateList.valueOf(themeColor))
        setExclusiveSelection(
            binding.fontIntaliana,
            binding.fontLeckerli,
            binding.fontMargarine,
            binding.fontLobster,
            binding.fontRethink,
            binding.fontPacifico,
        ) { selectedView ->
            val fontName = selectedView.tag as? String
            fontSelectionListener.invoke(fontName.orEmpty())
            Log.d("FontSelected", "Selected font: $fontName")
        }
        setExclusiveSelection(
            binding.icStartLine, binding.icCenterLine, binding.icEndLine
        ) { selectedView ->
            when (selectedView.id) {
                R.id.icStartLine -> textAlignmentListener.invoke("left")
                R.id.icCenterLine -> textAlignmentListener.invoke("center")
                R.id.icEndLine -> textAlignmentListener.invoke("right")
            }
        }
        setExclusiveSelectionHeadingSize(
            binding.icHeadingOne, binding.icHeadingTwo, binding.icHeadingThree
        ) { selectedView ->
            when (selectedView.id) {
                R.id.icHeadingOne -> textBoldListener.invoke(0)
                R.id.icHeadingTwo -> textBoldListener.invoke(1)
                R.id.icHeadingThree -> textBoldListener.invoke(2)
            }
        }
        setExclusiveSelectionColor(
            selectedTint = ContextCompat.getColor(requireContext(), R.color.app_primary_color),
            unselectedTint = ContextCompat.getColor(requireContext(), R.color.track_color),
            views = arrayOf(
                binding.icBlackColor,
                binding.icDarkGrayColor,
                binding.icLightGrayColor,
                binding.icPinkColor,
                binding.icGreenishColor,
                binding.icPurpleColor
            ),
            colors = colorPalette
        ) { selectedView, selectedColor ->
            textColorListener(selectedColor)
        }
        ivClose.setOnClickListener {
            imageDialog.dismiss()
            closeDialog.invoke()
        }
        imageDialog.setOnDismissListener {
            closeDialog.invoke()
        }
        binding.icBlueColor.setOnClickListener {
            ColorPickerDialog.Builder(requireContext()).setTitle(getString(R.string.colorpicker))
                .setPreferenceName(getString(R.string.mycolorpickerdialog)).setPositiveButton(
                    getString(R.string.confirm), ColorEnvelopeListener { envelope, fromUser ->
                        textColorListener.invoke(envelope.color)
                        imageDialog.dismiss()
                    }).setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }.attachAlphaSlideBar(true).attachBrightnessSlideBar(true).setBottomSpace(12).show()
        }


    }
}
fun Fragment.navigateToPlayStore() {
    val packageName = requireContext().packageName
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: Exception) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}
fun Fragment.sendEmailFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("cisco7865@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "App Feedback")
        putExtra(Intent.EXTRA_TEXT, "")
    }
    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}
inline fun Fragment.showFeedBackDialog(
    crossinline selectedEmotion: (EmojiInfo) -> Unit
) {
    val binding = FeedbackLayoutBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._15sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        btnCancel.setOnClickListener { imageDialog.dismiss() }

        btnDone.setOnClickListener {
            val rating = ratingBar.rating // Assuming ID is ratingBar

            if (rating > 3) {
                navigateToPlayStore()
            } else {
                sendEmailFeedback()
            }

            imageDialog.dismiss()
        }
    }
}

inline fun Fragment.editTagDialog(
    oldTags: List<CustomTagEntity>,
    selectedTag: CustomTagEntity,
    crossinline onUpdateTag: (List<CustomTagEntity>) -> Unit
) {

    val binding = EditTagDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {
        editTags.setText(selectedTag.tagName)
        editTags.setSelection(selectedTag.tagName.length)
        btnCancel.setOnClickListener {
            imageDialog.dismiss()
        }
        btnDone.setOnClickListener {
            val updatedText = editTags.text.toString().trim()
            if (updatedText.isNotEmpty()) {
                val updatedTag = selectedTag.copy(tagName = updatedText)
                val updatedList = oldTags.map { tag ->
                    if (tag.tagName == selectedTag.tagName) updatedTag else tag
                }
                onUpdateTag.invoke(updatedList)
                imageDialog.dismiss()
            } else {
                Toast.makeText(
                    context,
                    context?.getString(R.string.please_enter_valid_text),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}


inline fun Fragment.pickPhotDialog(
    sessionManagerRepo: SessionManagerRepo,
    crossinline cameraCallBack: (Boolean) -> Unit,
    crossinline galleryCallBack: (Boolean) -> Unit,
    crossinline onDismiss: () -> Unit,
) {

    val binding = PickPhotoDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)
    val themeColor = getCurrentThemeColor(sessionManagerRepo)
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._25sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
    }

    binding.apply {

        ivClose.backgroundTintList = ColorStateList.valueOf(themeColor)

        // If buttons exist
        icAudio.imageTintList = ColorStateList.valueOf(themeColor)
        icMusic.imageTintList = ColorStateList.valueOf(themeColor)
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
fun Fragment.getCurrentThemeColor( sessionManagerRepo: SessionManagerRepo): Int {
    val themeResId = sessionManagerRepo.getBgTheme()
    return when (themeResId) {
        R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
        R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
        R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
        R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
        R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
        else -> ContextCompat.getColor(requireContext(), R.color.app_primary_color)
    }
}



