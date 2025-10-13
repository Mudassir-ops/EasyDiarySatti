package com.example.easydiarysatti.utills

import android.app.Dialog
import android.app.TimePickerDialog
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.DialogBackgroundBinding
import com.example.easydiarysatti.databinding.DialogDateTimePickerBinding
import com.example.easydiarysatti.databinding.DialogImageviewBinding
import com.example.easydiarysatti.databinding.EditFeelingsDialogBinding
import com.example.easydiarysatti.databinding.EditTextDialogBinding
import com.example.easydiarysatti.databinding.FeedbackLayoutBinding
import com.example.easydiarysatti.domain.model.EmojiInfo
import com.example.easydiarysatti.saveBitmapToUri
import com.example.easydiarysatti.setExclusiveSelection
import com.example.easydiarysatti.setExclusiveSelectionColor
import com.example.easydiarysatti.setExclusiveSelectionHeadingSize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

inline fun Fragment.showEditFeelingsDialog(
    crossinline selectedEmotion: (EmojiInfo) -> Unit
) {
    val binding = EditFeelingsDialogBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)

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


        val emojiMap = mapOf(
            ivEmojiHappy to EmojiInfo(
                drawableRes = R.drawable.emoji_happy,
                colorHex = "#42ABD0",
                name = "Happy",
                tagColor = "#9870E2"
            ), ivEmojiCalm to EmojiInfo(
                drawableRes = R.drawable.emooji_calm,
                colorHex = "#5EE3A9",
                name = "Calm",
                tagColor = "#981B9C"
            ), ivEmojiSad to EmojiInfo(
                drawableRes = R.drawable.emoji_sad,
                colorHex = "#FFDE8B",
                name = "Sad",
                tagColor = "#848D9B"
            ), ivEmojiExcited to EmojiInfo(
                drawableRes = R.drawable.emooji_excited,
                colorHex = "#FF8D95",
                name = "Excited",
                tagColor = "#F8B903"
            ), ivEmojiAngry to EmojiInfo(
                drawableRes = R.drawable.emooji_angry,
                colorHex = "#FFAC81",
                name = "Angry",
                tagColor = "#475569"
            ), ivEmojiPlayful to EmojiInfo(
                drawableRes = R.drawable.emooji_playful,
                colorHex = "#A29DFB",
                name = "Playful",
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
    adapterMultiImageAdapter: MultiImageAdapter, crossinline closeDialog: () -> Unit
) {
    val context = this.context ?: return
    val binding = DialogBackgroundBinding.inflate(LayoutInflater.from(context))
    val imageDialog = Dialog(context)

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
    colorPalette: List<Int>,
    crossinline closeDialog: () -> Unit,
    crossinline fontSelectionListener: (fontName: String) -> Unit,
    crossinline textAlignmentListener: (alignment: String) -> Unit,
    crossinline textBoldListener: (fontSize: Int) -> Unit,
    crossinline textColorListener: (color: Int) -> Unit,
) {
    val binding = EditTextDialogBinding.inflate(LayoutInflater.from(context ?: return))
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
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }

    binding.apply {
        setExclusiveSelection(
            binding.fontIntaliana,
            binding.fontLeckerli,
            binding.fontMargarine,
            binding.fontLobster,
            binding.fontRethink,
            binding.fontPacifico
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
            binding.icHeadingOne,
            binding.icHeadingTwo,
            binding.icHeadingThree
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
    }
}


inline fun Fragment.showDatePicker(
    crossinline selectedDateTime: (String) -> Unit
) {
    val binding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(context ?: return))
    val imageDialog = Dialog(context ?: return)

    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        window?.apply {
            val params = WindowManager.LayoutParams()
            params.copyFrom(attributes)
            val displayMetrics = context.resources.displayMetrics
            val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._20sdp)
            params.width = displayMetrics.widthPixels - 2 * horizontalMargin
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }

    val calendar = Calendar.getInstance()

    binding.apply {
        ivClose.setOnClickListener { imageDialog.dismiss() }
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)

                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val formattedDateTime = formatter.format(calendar.time)

                    selectedDateTime(formattedDateTime)
                    imageDialog.dismiss()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }
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

        btnCancel.setOnClickListener {
            imageDialog.dismiss()
        }

        btnDone.setOnClickListener {
            imageDialog.dismiss()
            activity?.finishAffinity()
        }
    }
}