package com.example.easydiarysatti.ui.previewnote

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.addTags
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.databinding.FragmentPreviewBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.getHeadingSize
import com.example.easydiarysatti.setFont
import com.example.easydiarysatti.setHeadingSize
import com.example.easydiarysatti.setTextAlignmentByName
import com.example.easydiarysatti.ui.createnote.ImagesItemAdapter
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PreviewFragment : Fragment(R.layout.fragment_preview) {
    private val binding by viewBinding(FragmentPreviewBinding::bind)
    private val viewModel by activityViewModels<PreviewViewModel>()
    private val imagesItemAdapter: ImagesItemAdapter by lazy {
        ImagesItemAdapter(
            onNoteItemClick = { note -> },
            fromPreview = true
        )
    }
    private var noteId = 0L
    private var fromHome = false

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteId = arguments?.getLong(NOTE_ID) ?: 0L
        fromHome = arguments?.getBoolean(FROM_SCREEN) ?: false
        viewModel.getNoteById(noteId = noteId)
        setupImagesRecyclerview()
        observeNote()
        clickListener()
        setupBgTheme()
    }

    private fun clickListener() {
        binding?.apply {
        }
    }

    private fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allNotesPreviewState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    it.setupDefaultValues()
                }
        }
    }

    private fun setupImagesRecyclerview() {
        binding?.rvNotesImages?.run {
            adapter = imagesItemAdapter
            hasFixedSize()
        }
    }

    fun List<CustomTagEntity>.setupFlexBox() {
        binding?.flexboxLayout?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            addTags(
                fromPreview = true,
                this@setupFlexBox as MutableList<CustomTagEntity>,
                onTagClick = {},
                onRemoveTagClick = { tag -> })
        }
    }

    fun CreateNoteEntity?.setupDefaultValues() {
        binding?.apply {
            etHeader.setText(this@setupDefaultValues?.title)
            etDescription.setText(this@setupDefaultValues?.description)
            imagesItemAdapter.submitList(this@setupDefaultValues?.images ?: emptyList())
            this@setupDefaultValues?.tags?.setupFlexBox()
            ivEmoji.setImageResource(this@setupDefaultValues?.feelingEmojiRes ?: return@apply)
            this@setupDefaultValues.textColor?.let { etHeader.setTextColor(it) }
            this@setupDefaultValues.textColor?.let { etDescription.setTextColor(it) }
            val fontSizePair = this@setupDefaultValues.textFontSize?.toInt()?.let {
                this@setupDefaultValues.textFontSize.toInt().let { index ->
                    Pair(
                        (getHeadingSize(it) + 3F), (getHeadingSize(index) + 8F)
                    )
                }
            }
            fontSizePair?.first?.let {
                etHeader.setHeadingSize(
                    textSizeInSp = it
                )
            }
            fontSizePair?.second?.let {
                etDescription.setHeadingSize(
                    textSizeInSp = it
                )
            }
            this@setupDefaultValues.textAlignment?.let { setTextAlignmentByName(etHeader, it) }
            this@setupDefaultValues.textAlignment?.let {
                setTextAlignmentByName(
                    etDescription,
                    it
                )
            }
            this@setupDefaultValues.textFont?.let { etHeader.setFont(it, context ?: return) }
            this@setupDefaultValues.textFont?.let { etDescription.setFont(it, context ?: return) }
        }
    }

    private fun setupBgTheme() {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }


}