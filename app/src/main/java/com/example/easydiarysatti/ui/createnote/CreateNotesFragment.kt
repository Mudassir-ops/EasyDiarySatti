package com.example.easydiarysatti.ui.createnote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ENTITY
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
import com.example.easydiarysatti.setReminderEasyDiary
import com.example.easydiarysatti.setStyledDateAlreadyTime
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.setTextAlignmentByName
import com.example.easydiarysatti.showDatePickerWithTime
import com.example.easydiarysatti.showToast
import com.example.easydiarysatti.toFormattedString
import com.example.easydiarysatti.utills.showDatePicker
import com.example.easydiarysatti.utills.showEditFeelingsDialog
import com.example.easydiarysatti.viewBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class CreateNotesFragment : Fragment(R.layout.fragment_create_notes) {

    private val binding by viewBinding(FragmentCreateNotesBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private var createNoteEntity: CreateNoteEntity? = null
    private val imagesItemAdapter: ImagesItemAdapter by lazy {
        ImagesItemAdapter(onNoteItemClick = { note -> })
    }
    private var isNoteInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNoteEntity = CreateNoteEntity(
            feelingEmojiRes = R.drawable.emooji_excited,
            selectedEmojiColor = "#FF8D95",
            feelingTitle = "Excited",
            tagColor = "#F8B903"
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNote()
        observeNoteAction()
        clickListeners()
        adjustScreenKeyboard()
        setupImagesRecyclerview()
        listOf("Personal").setupFlexBox()
        setStyledDateTime(binding?.tvDate ?: return, R.color.black)
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
                        selectedEmojiColor = emojiInfo.colorHex,
                        feelingTitle = emojiInfo.name,
                        tagColor = emojiInfo.tagColor
                    )
                })
            }
            tvDate.setOnClickListener {
                showDatePickerWithTime { selectedCalendar ->
                    val uniqueId = UUID.randomUUID().hashCode()
                    if (selectedCalendar.timeInMillis <= System.currentTimeMillis()) {
                        selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    activity.setReminderEasyDiary(
                        calendar = selectedCalendar,
                        text = "Mudassir Here",
                        uniqueId = uniqueId
                    )
                    val formattedDate =
                        selectedCalendar.time.toFormattedString("dd/MM/yyyy hh:mm a")
                    setStyledDateAlreadyTime(
                        tvDate = tvDate,
                        colorId = R.color.black,
                        formatted = formattedDate
                    )
                    showToast(requireContext(), "Reminder set for $formattedDate")
                }
            }
            ivBottomArrow.setOnClickListener {
                showDatePicker(selectedDateTime = {

                })
            }
        }
    }

    fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .distinctUntilChanged().filterNotNull()
                .collect { note ->
                    if (!isNoteInitialized) {
                        note.setupDefaultValues()
                        isNoteInitialized = true
                        Log.e("observeNote", "observeNote:Null Note$note ")
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

                        is CreateNotesState.ImagePicked -> {
                            val lastSavedNotesImages =
                                viewModel.addImage(imagePath = note.imageUri.toString())
                            createNoteEntity = createNoteEntity?.copy(images = lastSavedNotesImages)
                            imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
                        }

                        is CreateNotesState.AddTag -> {
                            val lastSavedNotesTags = viewModel.addTag(tag = note.tag.toString())
                                .toMutableList()
                                .apply { if (!contains("Personal")) add("Personal") }
                            createNoteEntity =
                                createNoteEntity?.copy(tags = lastSavedNotesTags.reversed())

                            Log.e(
                                "observeNoteAction",
                                "observeNoteAction:${Gson().toJson(createNoteEntity)} ",
                            )
                            createNoteEntity?.setupDefaultValues()
                        }

                        CreateNotesState.TagAction -> {
                            createNoteEntity = createNoteEntity?.copy(
                                title = binding?.etHeader?.text?.toString().orEmpty(),
                                description = binding?.etDescription?.text?.toString().orEmpty()
                            )
                            findNavController().safeNav(
                                currentDestId = R.id.createNotesFragment,
                                actionId = R.id.action_createNotesFragment_to_addTagsFragment2,
                                Bundle().apply {
                                    putParcelable(NOTE_ENTITY, createNoteEntity)
                                    putBoolean(FROM_SCREEN, false)
                                }
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
                        else -> Unit
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
            createNoteEntity = createNoteEntity?.copy(textFont = fontName)
        }
    }

    private fun setAlignment(alignment: CreateNotesState.TextAlignment) {
        binding?.apply {
            setTextAlignmentByName(etHeader, alignment.alignment)
            setTextAlignmentByName(etDescription, alignment.alignment)
            createNoteEntity = createNoteEntity?.copy(textAlignment = alignment.alignment)
        }
    }

    private fun setFontSize(headingSize: CreateNotesState.HeadingSize) {
        val fontSizePair = Pair(
            (getHeadingSize(headingSize.headingSize) + 3F),
            (getHeadingSize(headingSize.headingSize) + 8F)
        )
        createNoteEntity = createNoteEntity?.copy(textFontSize = headingSize.headingSize.toString())
        Log.e("setFontSize", "setTextColor: SelecetdColor$headingSize")
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
            createNoteEntity = createNoteEntity?.copy(textColor = textColor.textColor)
        }
    }

    fun CreateNoteEntity.setupDefaultValues() {
        createNoteEntity = this
        viewModel.clearTags()
        viewModel.clearImages()
        viewModel.addImages(imagePath = createNoteEntity?.images ?: listOf())
        viewModel.addTags(tags = createNoteEntity?.tags ?: listOf())
        binding?.apply {
            etHeader.setText(createNoteEntity?.title)
            etDescription.setText(createNoteEntity?.description)
            imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
            if (createNoteEntity?.tags?.isEmpty() == false) {
                if (createNoteEntity?.tags?.contains("Personal") == false) {
                    createNoteEntity?.tags?.toMutableSet()?.add("Personal")
                }
                createNoteEntity?.tags?.setupFlexBox()
            } else {
                listOf("Personal").setupFlexBox()
            }
            ivEmoji.setImageResource(createNoteEntity?.feelingEmojiRes ?: return@apply)
            nestedScrollView.loadBackground(
                resourceId = createNoteEntity?.backgroundRes,
                placeholder = R.drawable.theme_1
            )
            createNoteEntity?.textColor?.let { etHeader.setTextColor(it) }
            createNoteEntity?.textColor?.let { etDescription.setTextColor(it) }
            val fontSizePair = createNoteEntity?.textFontSize?.toInt()?.let {
                createNoteEntity?.textFontSize?.toInt()?.let { index ->
                    Pair(
                        (getHeadingSize(it) + 3F),
                        (getHeadingSize(index) + 8F)
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
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etHeader, it) }
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etDescription, it) }
            createNoteEntity?.textFont?.let { etHeader.setFont(it, context ?: return) }
            createNoteEntity?.textFont?.let { etDescription.setFont(it, context ?: return) }
        }
    }
}