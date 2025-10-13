package com.example.easydiarysatti.ui.createnote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.addTags
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentCreateNotesBinding
import com.example.easydiarysatti.enableResize
import com.example.easydiarysatti.getHeadingSize
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setFont
import com.example.easydiarysatti.setHeadingSize
import com.example.easydiarysatti.setKeyboardVisibilityListenerCreateNote
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.setTextAlignmentByName
import com.example.easydiarysatti.utills.showDatePicker
import com.example.easydiarysatti.utills.showEditFeelingsDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateNotesFragment : Fragment(R.layout.fragment_create_notes) {

    private val binding by viewBinding(FragmentCreateNotesBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private var createNoteEntity: CreateNoteEntity? = null
    private val imagesItemAdapter: ImagesItemAdapter by lazy {
        ImagesItemAdapter(onNoteItemClick = { note -> })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNoteEntity = CreateNoteEntity(
            feelingEmojiRes = R.drawable.emooji_excited,
            textColor = "#FF8D95",
            feelingTitle = "Excited",
            tagColor = "#F8B903"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            observeNote()
            observeNoteAction()
            clickListeners()
            adjustScreenKeyboard()
            setupImagesRecyclerview()
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                if (findNavController().popBackStack()) {
                    findNavController().navigateUp()
                } else {
                    isEnabled = false
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            setStyledDateTime(binding?.tvDate ?: return@launch, R.color.black)
        }
    }

    fun List<String>.setupFlexBox() {
        binding?.flexboxLayout?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            addTags(
                this@setupFlexBox as MutableList<String>,
                onTagClick = {},
                onRemoveTagClick = { tag ->
                    viewModel.removeTag(tag = tag)
                })
        }
    }

    @OptIn(FlowPreview::class)
    fun clickListeners() {
        binding?.apply {
            ivEmoji.setOnClickListener {
                showEditFeelingsDialog(selectedEmotion = { emojiInfo ->
                    ivEmoji.setImageResource(emojiInfo.drawableRes)
                    createNoteEntity = createNoteEntity?.copy(
                        feelingEmojiRes = emojiInfo.drawableRes,
                        textColor = emojiInfo.colorHex,
                        feelingTitle = emojiInfo.name,
                        tagColor = emojiInfo.tagColor
                    )
                })
            }
            tvDate.setOnClickListener {
                showDatePicker(selectedDateTime = {

                })
            }
            ivBottomArrow.setOnClickListener {
                showDatePicker(selectedDateTime = {

                })
            }
        }
    }

    fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { note ->
                note?.let {
                    createNoteEntity = null
                    createNoteEntity = it
                    viewModel.clearTags()
                    viewModel.clearImages()
                    viewModel.addImages(imagePath = createNoteEntity?.images ?: listOf())
                    viewModel.addTags(tags = createNoteEntity?.tags ?: listOf())
                    binding?.apply {
                        etHeader.setText(createNoteEntity?.title)
                        etDescription.setText(createNoteEntity?.description)
                        imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
                        if (createNoteEntity?.tags?.isEmpty() == false) {
                            createNoteEntity?.tags?.setupFlexBox()
                        } else {
                            listOf("Personal").setupFlexBox()
                        }
                        ivEmoji.setImageResource(createNoteEntity?.feelingEmojiRes ?: return@apply)
                        nestedScrollView.loadBackground(
                            resourceId = createNoteEntity?.backgroundRes,
                            placeholder = R.drawable.theme_1
                        )
                    }
                } ?: run {
                    createNoteEntity = CreateNoteEntity(
                        feelingEmojiRes = R.drawable.emooji_excited,
                        textColor = "#FF8D95",
                        feelingTitle = "Excited",
                        tagColor = "#F8B903",
                        tags = listOf(),
                        images = listOf(),
                    )
                    listOf("Personal").setupFlexBox()
                    binding?.apply {
                        etHeader.setText(createNoteEntity?.title)
                        etDescription.setText(createNoteEntity?.description)
                        imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
                    }
                    Log.e("observeNote", "observeNote:Null Note ")
                }
            }
        }
    }

    fun observeNoteAction() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notesActionState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { note ->
                    Log.e("headerSaveSatti", "setClickListeners:$note ")
                    when (note) {
                        CreateNotesState.SaveNote -> {
                            saveNote()
                        }

                        CreateNotesState.DiscardNote -> {

                        }

                        is CreateNotesState.ShowMessage -> {

                        }

                        CreateNotesState.BackAction -> findNavController().navigateUp()
                        is CreateNotesState.ImagePicked -> {
                            val lastSavedNotesImages =
                                viewModel.addImage(imagePath = note.imageUri.toString())
                            createNoteEntity = createNoteEntity?.copy(images = lastSavedNotesImages)
                            imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
                        }

                        is CreateNotesState.AddTag -> {
                            val lastSavedNotesTags = viewModel.addTag(tag = note.tag.toString())
                            createNoteEntity = createNoteEntity?.copy(tags = lastSavedNotesTags)
                            Log.e(
                                "MudassirSattiTag-->",
                                "observeNoteAction: ${createNoteEntity?.tags}",
                            )
                            createNoteEntity?.tags?.setupFlexBox()
                        }

                        CreateNotesState.TagAction -> {
                            viewModel.setupNoteEntity(createNoteEntity = createNoteEntity)
                            findNavController().safeNav(
                                currentDestId = R.id.createNotesFragment,
                                actionId = R.id.action_createNotesFragment_to_addTagsFragment2,
                            )
                        }

                        is CreateNotesState.ChangeBg -> {
                            createNoteEntity =
                                createNoteEntity?.copy(backgroundRes = note.bgImageRes)
                            binding?.nestedScrollView?.loadBackground(
                                resourceId = createNoteEntity?.backgroundRes
                            )
                        }

                        is CreateNotesState.FontAction -> onFontSelected(note.font)
                        is CreateNotesState.TextAlignment -> setAlignment(alignment = note)
                        is CreateNotesState.HeadingSize -> setFontSize(headingSize = note)
                        is CreateNotesState.TextColor -> setTextColor(textColor = note)
                    }
                }
        }
    }

    private fun saveNote() {
        createNoteEntity = createNoteEntity?.copy(
            title = binding?.etHeader?.text?.toString().orEmpty(),
            description = binding?.etDescription?.text?.toString().orEmpty()
        )
        createNoteEntity?.let { viewModel.mergeAndSave(createNoteEntity = it) } ?: run {
            Log.e("headerSaveSatti", "setClickListeners:$createNoteEntity is Null ")
        }
        findNavController().navigateUp()
    }

    private fun setupImagesRecyclerview() {
        binding?.rvNotesImages?.run {
            adapter = imagesItemAdapter
            hasFixedSize()
        }
    }

    private fun adjustScreenKeyboard() {
        setKeyboardVisibilityListenerCreateNote { isVisible ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (isVisible) {
                    enableResize(true)
                    binding?.nestedScrollView?.post {
                        if (view != null && viewLifecycleOwner.lifecycle.currentState.isAtLeast(
                                Lifecycle.State.STARTED
                            )
                        ) {
                            binding?.nestedScrollView?.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                } else {
                    enableResize(false)
                }
            }
        }
    }

    private fun onFontSelected(fontName: String) {
        binding?.apply {
            etHeader.setFont(fontName, context ?: return)
            etDescription.setFont(fontName, context ?: return)
        }
    }

    private fun setAlignment(alignment: CreateNotesState.TextAlignment) {
        binding?.apply {
            setTextAlignmentByName(etHeader, alignment.alignment)
            setTextAlignmentByName(etDescription, alignment.alignment)
        }
    }

    private fun setFontSize(headingSize: CreateNotesState.HeadingSize) {
        val fontSizePair = Pair(
            (getHeadingSize(headingSize.headingSize) + 0F),
            (getHeadingSize(headingSize.headingSize) + 3F)
        )
        binding?.apply {
            etHeader.setHeadingSize(
                textSizeInSp = fontSizePair.first
            )
            etDescription.setHeadingSize(
                textSizeInSp = fontSizePair.second
            )
        }
    }

    private fun setTextColor(textColor: CreateNotesState.TextColor) {
        binding?.apply {
            etHeader.setTextColor(textColor.textColor)
            etDescription.setTextColor(textColor.textColor)
        }
    }

}