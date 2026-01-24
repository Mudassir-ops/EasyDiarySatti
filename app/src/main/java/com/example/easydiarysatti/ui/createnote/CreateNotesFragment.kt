package com.example.easydiarysatti.ui.createnote

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
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
import com.example.easydiarysatti.ads.interstitial.InterstitialAdsConfig
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.interstitial.enums.InterAdKey
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.databinding.FragmentCreateNotesBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
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
import com.example.easydiarysatti.utills.getCurrentThemeColor
import com.example.easydiarysatti.utills.showEditFeelingsDialog
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

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
            }, imagesCount = {
                viewModel.imagesCount = it
            }
        )
    }
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    @Inject
    lateinit var interstitialAdsConfig: InterstitialAdsConfig

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    private var isNoteInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        createNoteEntity = CreateNoteEntity(
            feelingEmojiRes = R.drawable.happy,
            selectedEmojiColor = "#FF8D95",
            feelingTitle = "Excited",
            tagColor = "#F8B903",
            tags = emptyList()
        )
    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        val params = Bundle().apply { putString("action_label", label) }
        FirebaseAnalytics.getInstance(requireContext()).logEvent(eventName, params)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNote()
        observeNoteAction()
        clickListeners()
        adjustScreenKeyboard()
        setupImagesRecyclerview()
        setupDescriptionScroll()
        logAnalyticsEvent("Add_Note_Screen", "fragment_open")
        listOf(
            CustomTagEntity(
                tagName = "", noteId = 999
            )
        ).setupFlexBox()
        val themeColor=getCurrentThemeColor(sessionManagerRepo)
        binding?.ivBottomArrow?.imageTintList=ColorStateList.valueOf(themeColor)
        setStyledDateTime(binding?.tvDate ?: return, R.color.grey)
        loadInterstitial()

    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDescriptionScroll() {
        binding?.etDescription?.setOnTouchListener { view, event ->
            if (view.hasFocus()) {
                // Disallow NestedScrollView to intercept touch events
                view.parent.requestDisallowInterceptTouchEvent(true)

                // Check if the event is an ACTION_UP to return control to the parent
                if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }
    fun List<CustomTagEntity>.setupFlexBox() {
        val displayTags = this.filter { it.tagName.isNotBlank() }

        if (displayTags.isEmpty()) {
            binding?.flexboxLayout?.visibility = View.GONE
            binding?.flexboxLayout?.removeAllViews() // Clear UI
            return
        }

        binding?.flexboxLayout?.apply {
            removeAllViews()
            visibility = View.VISIBLE
            addTags(
                fromPreview = false,
                displayTags.toMutableList(),
                onTagClick = {},
                onRemoveTagClick = { tagToRemove ->
                    // 1. Remove from ViewModel (this returns the new updated list)
                    val updatedTags = viewModel.removeTag(tag = tagToRemove)

                    // 2. Update the fragment's local entity
                    createNoteEntity = createNoteEntity?.copy(tags = updatedTags)

                    // 3. Re-run setupFlexBox with the new list to refresh UI
                    updatedTags.setupFlexBox()

                    // 4. Sync with Database if the note already exists
                    if (createNoteEntity?.noteId != 0L) {
                        viewModel.updateTagsForNote(
                            noteId = createNoteEntity?.noteId ?: 0L,
                            newTags = updatedTags
                        )
                    }
                }
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun clickListeners() {
        binding?.apply {
            ivEmoji.setOnClickListener {
                logAnalyticsEvent("Add_Note_Emoji_Clicked", "icon_click")
                showEditFeelingsDialog( sessionManagerRepo = sessionManagerRepo,
                    selectedEmotion = { emojiInfo ->
                        logAnalyticsEvent("Add_Note_Emoji_Selected", emojiInfo.name)
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
                logAnalyticsEvent("Add_Note_Reminder_Clicked", "date_text_click")
                onClickDateTimePick()
            }
            ivBottomArrow.setOnClickListener {
                logAnalyticsEvent("Add_Note_Reminder_Clicked", "arrow_click")
                onClickDateTimePick()
            }
            binding?.icSwitchRemainder?.setOnClickListener {
                logAnalyticsEvent("Add_Note_Reminder_Clicked", "switch_click")
                binding?.icSwitchRemainder?.isChecked = false
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
                                        any { it.tagName.equals("", ignoreCase = true) }
                                    if (!hasPersonal) add(
                                        CustomTagEntity(
                                            tagName = "",
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
        checkInterstitial()
    }

    private fun setupImagesRecyclerview() {
        binding?.rvNotesImages?.run {
            adapter = imagesItemAdapter
            hasFixedSize()
        }
    }

    private fun adjustScreenKeyboard() {
        setKeyboardVisibilityListenerCreateNote { isVisible ->
            if (!isAdded || view == null || viewLifecycleOwner.lifecycle.currentState < Lifecycle.State.STARTED) {
                return@setKeyboardVisibilityListenerCreateNote
            }
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
            addDefaultTags()
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
            showDatePickerWithTime(
                sessionManagerRepo = sessionManagerRepo,
                calendar = Calendar.getInstance()
            ){ selectedCalendar ->
                if (!isAdded || view == null ||
                    viewLifecycleOwner.lifecycle.currentState < Lifecycle.State.STARTED
                ) return@showDatePickerWithTime
                if (binding?.etHeader?.text.toString().isEmpty()) {
                    binding?.parentLayout?.showSnackbar(getString(R.string.required_title))
                    return@showDatePickerWithTime
                }
                val uniqueId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
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
                    uniqueId = uniqueId,
                    contentTitle = getString(R.string.note_reminder)
                )
                binding?.icSwitchRemainder?.isChecked = true
            }
        }
    }

    fun addDefaultTags() {
        // 1. Get existing tags from the entity
        val tags = createNoteEntity?.tags?.toMutableList() ?: mutableListOf()

        // 2. Filter out any blank tags that might have been saved accidentally
        val validTags = tags.filter { it.tagName.isNotBlank() }

        // 3. Sync with ViewModel
        viewModel.clearTags()
        if (validTags.isNotEmpty()) {
            viewModel.addTags(validTags)
        }

        // 4. Update the local entity reference
        createNoteEntity = createNoteEntity?.copy(tags = validTags)

        // 5. Only show the flexbox if there is actually something to display
        if (validTags.isNotEmpty()) {
            binding?.flexboxLayout?.visibility = View.VISIBLE
            validTags.setupFlexBox()
        } else {
            binding?.flexboxLayout?.visibility = View.GONE
        }
    }

    private fun loadInterstitial() {
        interstitialAdsConfig.loadInterstitialAd(InterAdKey.FEATURE_SAVE_NOTE)
    }

    private fun checkInterstitial() {
        when (interstitialAdsConfig.isInterstitialLoaded()) {
            true -> showInterstitial()
            false -> navigateScreen()
        }
    }

    private fun showInterstitial() {
        // Set bypass to true so MainActivity.onResume doesn't show login
        sessionManagerRepo.bypassSecurityLogin(true)

        interstitialAdsConfig.showInterstitialAd(
            requireActivity(),
            InterAdKey.FEATURE_SAVE_NOTE,
            object : InterstitialOnShowCallBack {
                override fun onAdFailedToShow() {
                    sessionManagerRepo.bypassSecurityLogin(false)
                    navigateScreen()
                }
                override fun onAdImpressionDelayed() {
                    // Keep it true if the ad is still technically showing/active
                    navigateScreen()
                }
                // If your callback has an 'onAdDismissed' or similar,
                // set bypassSecurityLogin(false) there.
            })
    }

    private fun navigateScreen() {
        findNavController().navigateUp()
    }
}