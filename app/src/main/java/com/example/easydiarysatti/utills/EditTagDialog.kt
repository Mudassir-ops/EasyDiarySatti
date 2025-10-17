package com.example.easydiarysatti.utills

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import com.example.easydiarysatti.R
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.EditTagDialogBinding

class EditTagDialog(
    activity: Activity,
    private val label1: String,
    private val label2: String,
    private val label3: String,
    private val oldTags: List<CustomTagEntity>,
    private val selectedTag: CustomTagEntity,
    var onUpdateTag: ((List<CustomTagEntity>) -> Unit)? = null,
    var onCancelTag: (() -> Unit)? = null,
) : Dialog(activity) {

    private val inflater =
        activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val binding = EditTagDialogBinding.inflate(inflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        binding.apply {
            txtUploadAudio.text = label1
            editTags.hint = label2
            txtEdit.text = label3

            // prefill with current tag name
            editTags.setText(selectedTag.tagName)
            editTags.setSelection(selectedTag.tagName.length)

            txtCancel.setOnClickListener {
                onCancelTag?.invoke()
                dismiss()
            }

            txtEdit.setOnClickListener {
                val updatedText = editTags.text.toString().trim()
                if (updatedText.isNotEmpty()) {
                    val updatedTag = selectedTag.copy(tagName = updatedText)
                    val updatedList = oldTags.map { tag ->
                        if (tag.tagName == selectedTag.tagName) updatedTag else tag
                    }
                    onUpdateTag?.invoke(updatedList)
                    dismiss()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.please_enter_valid_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }
}


