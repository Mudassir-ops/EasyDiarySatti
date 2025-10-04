package com.example.easydiarysatti.utills

import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.EditFeelingsDialogBinding

inline fun Fragment.showEditFeelingsDialog(
    crossinline selectedEmotion: (Int, String) -> Unit
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
            ivEmojiHappy to (R.drawable.emoji_happy to "#42ABD0"),
            ivEmojiCalm to (R.drawable.emooji_calm to "#5EE3A9"),
            ivEmojiSad to (R.drawable.emoji_sad to "#FFDE8B"),
            ivEmojiExcited to (R.drawable.emooji_excited to "#FF9800"),
            ivEmojiAngry to (R.drawable.emooji_angry to "#FFAC81"),
            ivEmojiPlayful to (R.drawable.emooji_playful to "#A29DFB")
        )

        emojiMap.forEach { (view, pair) ->
            val (resId, colorHex) = pair
            view.setOnClickListener {
                selectedEmotion(resId, colorHex)
                imageDialog.dismiss()
            }
        }

        ivClose.setOnClickListener { imageDialog.dismiss() }
    }
}

