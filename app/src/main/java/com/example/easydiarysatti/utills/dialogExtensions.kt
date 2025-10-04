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
        // Each emoji has a drawable + a predefined color (as hex string)
        val emojiMap = mapOf(
            ivEmojiHappy to EmojiInfo(R.drawable.emoji_happy, "#42ABD0", "Happy"),
            ivEmojiCalm to EmojiInfo(R.drawable.emooji_calm, "#5EE3A9", "Calm"),
            ivEmojiSad to EmojiInfo(R.drawable.emoji_sad, "#FFDE8B", "Sad"),
            ivEmojiExcited to EmojiInfo(R.drawable.emooji_excited, "#FF8D95", "Excited"),
            ivEmojiAngry to EmojiInfo(R.drawable.emooji_angry, "#FFAC81", "Angry"),
            ivEmojiPlayful to EmojiInfo(R.drawable.emooji_playful, "#A29DFB", "Playful")
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

