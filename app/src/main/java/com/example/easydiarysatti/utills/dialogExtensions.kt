package com.example.easydiarysatti.utills

import android.app.Dialog
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.EditFeelingsDialogBinding

inline fun Fragment.showEditFeelingsDialog(
    crossinline selectedEmotion: (Int) -> Unit
) {
    val binding = EditFeelingsDialogBinding.inflate(LayoutInflater.from(this.context ?: return))
    val imageDialog = Dialog(this.context ?: return)
    imageDialog.run {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        this.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
        )
        val params = WindowManager.LayoutParams()
        params.copyFrom(window?.attributes)
        val displayMetrics = context.resources.displayMetrics
        val horizontalMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
        params.width = displayMetrics.widthPixels - 2 * horizontalMargin
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = params
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        show()
    }
    binding.apply {
        val emojiMap = mapOf(
            ivEmojiHappy to R.drawable.emoji_happy,
            ivEmojiCalm to R.drawable.emooji_calm,
            ivEmojiSad to R.drawable.emoji_sad,
            ivEmojiExcited to R.drawable.emooji_excited,
            ivEmojiAngry to R.drawable.emooji_angry,
            ivEmojiPlayful to R.drawable.emooji_playful
        )

        emojiMap.forEach { (view, resId) ->
            view.setOnClickListener {
                selectedEmotion(resId)
                imageDialog.dismiss()
            }
        }
        ivClose.setOnClickListener {
            imageDialog.dismiss()
        }

    }

}
