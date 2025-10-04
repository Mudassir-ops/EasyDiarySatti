package com.example.easydiarysatti.utills

import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.EditFeelingsDialogBinding
import com.example.easydiarysatti.domain.model.EmojiInfo

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
            ),
            ivEmojiCalm to EmojiInfo(
                drawableRes = R.drawable.emooji_calm,
                colorHex = "#5EE3A9",
                name = "Calm",
                tagColor = "#981B9C"
            ),
            ivEmojiSad to EmojiInfo(
                drawableRes = R.drawable.emoji_sad,
                colorHex = "#FFDE8B",
                name = "Sad",
                tagColor = "#DB2256"
            ),
            ivEmojiExcited to EmojiInfo(
                drawableRes = R.drawable.emooji_excited,
                colorHex = "#FF8D95",
                name = "Excited",
                tagColor = "#F8B903"
            ),
            ivEmojiAngry to EmojiInfo(
                drawableRes = R.drawable.emooji_angry,
                colorHex = "#FFAC81",
                name = "Angry",
                tagColor = "#CD0C4E"
            ),
            ivEmojiPlayful to EmojiInfo(
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

