package com.example.easydiarysatti.ui.createnote

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.MainActivity
import com.example.easydiarysatti.NOTE_ENTITY
import com.example.easydiarysatti.R
import com.example.easydiarysatti.addTags
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.databinding.FragmentCreateNotesBinding
import com.example.easydiarysatti.enableResize
import com.example.easydiarysatti.getHeadingSize
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setFont
import com.example.easydiarysatti.setHeadingSize
import com.example.easydiarysatti.setKeyboardVisibilityListenerCreateNote
import com.example.easydiarysatti.setReminderEasyDiary
import com.example.easydiarysatti.setStyledDateAlreadyTime
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.setTextAlignmentByName
import com.example.easydiarysatti.showDatePickerWithTime
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.toFormattedString
import com.example.easydiarysatti.ui.remainder.RemainderViewModel
import com.example.easydiarysatti.utills.showEditFeelingsDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

@AndroidEntryPoint
class CreateNotesFragment : Fragment(R.layout.fragment_create_notes) {

    private val binding by viewBinding(FragmentCreateNotesBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()
    private val reminderViewModel by viewModels<RemainderViewModel>()
    private var createNoteEntity: CreateNoteEntity? = null
    private val imagesItemAdapter: ImagesItemAdapter by lazy {
        ImagesItemAdapter(
            fromPreview = false,
            onDeleteItemClick = { imageToDelete ->
                createNoteEntity = createNoteEntity?.copy(
                    images = viewModel.removeImage(imageToDelete)
                )
                AppLogger.createLog("ImagePicked", "${createNoteEntity?.images?.size}")
                val updatedList = imagesItemAdapter.currentList.toMutableList().apply {
                    remove(imageToDelete)
                }
                imagesItemAdapter.submitList(updatedList)
                val noteId = viewModel.noteState.value?.noteId ?: return@ImagesItemAdapter
                val currentImages =
                    viewModel.noteState.value?.images?.toMutableList() ?: mutableListOf()
                currentImages.remove(imageToDelete)
                viewModel.removeImageDb(
                    noteId = noteId,
                    imagesList = currentImages
                )
            }
        )
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
        listOf(
            CustomTagEntity(
                tagName = "Personal", noteId = 999
            )
        ).setupFlexBox()
        setStyledDateTime(binding?.tvDate ?: return, R.color.black)
    }

    fun List<CustomTagEntity>.setupFlexBox() {
        binding?.flexboxLayout?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            addTags(
                fromPreview = false,
                this@setupFlexBox as MutableList<CustomTagEntity>,
                onTagClick = {},
                onRemoveTagClick = { tag ->
                    if (createNoteEntity?.noteId != 0L) {
                        //remove from db also
                    }
                    viewModel.removeTag(tag = tag)
                })
        }
    }

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
                onClickDateTimePick()
            }
            ivBottomArrow.setOnClickListener {
                onClickDateTimePick()
            }
        }
    }

    fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.noteState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .distinctUntilChanged().filterNotNull().collect { note ->
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
                            AppLogger.createLog("ImagePicked", "${createNoteEntity?.images?.size}")
                            val lastSavedNotesImages =
                                viewModel.addImage(imagePath = note.imageUri.toString())
                            AppLogger.createLog("ImagePicked", "${lastSavedNotesImages.size}")
                            createNoteEntity = createNoteEntity?.copy(images = lastSavedNotesImages)
                            imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
                        }

                        is CreateNotesState.AddTag -> {
                            val lastSavedNotesTags = viewModel.addTag(
                                tag = note.tag.toString(),
                                noteId = note.createNoteEntity?.noteId?.toInt() ?: 0
                            ).toMutableList().apply {
                                if (this.isEmpty()) {
                                    val hasPersonal =
                                        any { it.tagName.equals("Personal", ignoreCase = true) }
                                    if (!hasPersonal) add(
                                        CustomTagEntity(
                                            tagName = "Personal",
                                            noteId = note.createNoteEntity?.noteId?.toInt() ?: 0
                                        )
                                    )
                                }
                            }
                            createNoteEntity =
                                createNoteEntity?.copy(tags = lastSavedNotesTags.reversed())
                            createNoteEntity?.setupDefaultValues()
                        }

                        CreateNotesState.TagAction -> {
                            createNoteEntity = createNoteEntity?.copy(
                                title = binding?.etHeader?.text?.toString().orEmpty(),
                                description = binding?.etDescription?.text?.toString().orEmpty()
                            )
                            findNavController().safeNav(
                                currentDestId = R.id.createNotesFragment,
                                actionId = R.id.action_createNotesFragment2_to_addTagsFragment,
                                Bundle().apply {
                                    putParcelable(NOTE_ENTITY, createNoteEntity)
                                    putBoolean(FROM_SCREEN, false)
                                })
                        }

                        is CreateNotesState.ChangeBg -> {
                            createNoteEntity =
                                createNoteEntity?.copy(backgroundRes = note.bgImageRes)

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
        Log.e("setupDefaultValues", "setupDefaultValues: ${createNoteEntity?.images}")
        viewModel.addImages(imagePath = createNoteEntity?.images ?: listOf())
        viewModel.addTags(tags = createNoteEntity?.tags ?: listOf())
        binding?.apply {
            etHeader.setText(createNoteEntity?.title)
            etDescription.setText(createNoteEntity?.description)
            imagesItemAdapter.submitList(createNoteEntity?.images ?: emptyList())
            if (!createNoteEntity?.tags.isNullOrEmpty()) {
                val tags = createNoteEntity?.tags?.toMutableList()
                if (tags.isNullOrEmpty()) {
                    val hasPersonal = tags?.any { it.tagName.equals("Personal", ignoreCase = true) }
                    if (hasPersonal == false) {
                        tags.add(
                            CustomTagEntity(
                                tagName = "Personal",
                                noteId = createNoteEntity?.noteId?.toInt() ?: 0
                            )
                        )
                    }
                }
                createNoteEntity = createNoteEntity?.copy(tags = tags)
                createNoteEntity?.tags?.setupFlexBox()
            } else {
                listOf(CustomTagEntity(tagName = "Personal", noteId = 999)).setupFlexBox()
            }
            ivEmoji.setImageResource(createNoteEntity?.feelingEmojiRes ?: return@apply)
            createNoteEntity?.textColor?.let { etHeader.setTextColor(it) }
            createNoteEntity?.textColor?.let { etDescription.setTextColor(it) }
            val fontSizePair = createNoteEntity?.textFontSize?.toInt()?.let {
                createNoteEntity?.textFontSize?.toInt()?.let { index ->
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
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etHeader, it) }
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etDescription, it) }
            createNoteEntity?.textFont?.let { etHeader.setFont(it, context ?: return) }
            createNoteEntity?.textFont?.let { etDescription.setFont(it, context ?: return) }
        }
    }

    fun onClickDateTimePick() {
        (activity as? MainActivity)?.requestExactAlarmPermission {
            showDatePickerWithTime { selectedCalendar ->
                if (binding?.etHeader?.text.toString().isEmpty()) {
                    binding?.parentLayout?.showSnackbar(getString(R.string.required_title))
                    return@showDatePickerWithTime
                }
                val uniqueId = UUID.randomUUID().hashCode()
                val now = System.currentTimeMillis()
                if (selectedCalendar.timeInMillis > now) {
                    val formattedDate =
                        selectedCalendar.time.toFormattedString("dd-MM-yy | h:mm a")
                    reminderViewModel.insertReminder(
                        ReminderEntity(
                            id = uniqueId,
                            description = getString(
                                R.string.don_t_forget_your_note,
                                binding?.etHeader?.text
                            ),
                            formattedDate = formattedDate,
                            scheduleAt = selectedCalendar.timeInMillis,
                            shouldPlay = false,
                            noteReminder = true
                        )
                    )
                    binding?.parentLayout?.showSnackbar(
                        getString(
                            R.string.reminder_set_for_time,
                            formattedDate
                        )
                    )
                    setStyledDateAlreadyTime(
                        tvDate = binding?.tvDate ?: return@showDatePickerWithTime,
                        colorId = R.color.black,
                        formatted = formattedDate
                    )
                } else {
                    selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    val formattedDate =
                        selectedCalendar.time.toFormattedString("dd-MM-yy | h:mm a")
                    reminderViewModel.insertReminder(
                        ReminderEntity(
                            id = uniqueId,
                            description = getString(
                                R.string.don_t_forget_your_note,
                                binding?.etHeader?.text
                            ),
                            formattedDate = formattedDate,
                            scheduleAt = selectedCalendar.timeInMillis,
                            shouldPlay = false,
                            noteReminder = true
                        )
                    )
                    setStyledDateAlreadyTime(
                        tvDate = binding?.tvDate ?: return@showDatePickerWithTime,
                        colorId = R.color.black,
                        formatted = formattedDate
                    )
                    binding?.parentLayout?.showSnackbar(getString(R.string.reminder_set_for_future_date))
                }
                activity.setReminderEasyDiary(
                    calendar = selectedCalendar,
                    text = getString(R.string.don_t_forget_your_note, binding?.etHeader?.text),
                    uniqueId = uniqueId
                )
            }
        }
    }
}