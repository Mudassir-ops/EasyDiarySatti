package com.example.easydiarysatti.ui.createnote

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.interstitial.InterstitialAdsConfig
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.interstitial.enums.InterAdKey
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.utils.addCleanView
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
import com.example.easydiarysatti.utills.InternetConnectivityDialog
import com.example.easydiarysatti.utills.SaveDraftDialog
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
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    private val reminderViewModel by viewModels<RemainderViewModel>()
    private var createNoteEntity: CreateNoteEntity? = null
    private val homeViewModel by activityViewModels<com.example.easydiarysatti.ui.home.HomeViewModel>()

    @Inject
    lateinit var internetManager: InternetManager
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

    lateinit var mFirebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var interstitialAdsConfig: InterstitialAdsConfig

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    @Inject
    lateinit var sharedPref: com.example.easydiarysatti.ads.manager.SharedPreferenceUtils

    /**
     * BillingManager for Remove Ads direct purchase.
     * Constructed here because CreateNotesFragment can show RemoveAdsDialog
     * from both showBackPressInterstitial() and showInterstitial().
     * onPurchaseSuccess mirrors the same logic as MainFragment.
     */
    private val billingManager: com.example.easydiarysatti.paywalls.BillingManager by lazy {
        com.example.easydiarysatti.paywalls.BillingManager(
            activity = requireActivity(),
            onPurchaseSuccess = { productId ->
                if (productId == com.example.easydiarysatti.paywalls.ProAccessManager.REMOVE_ADS_PRODUCT_ID) {
                    sharedPref.isAppPurchased = true
                }
            }
        )
    }

    /**
     * ProAccessManager — used here for onInterstitialCrossClicked() (both backpress and save inter).
     * billingManager is injected so RemoveAdsDialog CTA launches direct IN_APP purchase.
     * Shares the same interstitialCrossCount counter via SharedPreferences.
     */
    private val proAccessManager by lazy {
        com.example.easydiarysatti.paywalls.ProAccessManager(
            activity = requireActivity() as androidx.fragment.app.FragmentActivity,
            sharedPref = sharedPref,
            billingManager = billingManager
        )
    }

    private var isNoteInitialized = false

    // ✅ CORE FIX: Single navigation guard — ensures navigateScreen() fires AT MOST ONCE
    // per fragment instance regardless of how many callbacks (ad dismiss, fail, delay,
    // onAfterDismiss from RemoveAdsDialog) arrive. Without this, two callbacks arriving
    // close together both call navigateScreen(), the second one finds the fragment already
    // partially popped and navigates to CreateNote instead of Home.
    private var hasNavigated = false

    /**
     * Set to true by MainFragment.captureInnerDestBeforePaywall() immediately before
     * MainPaywallFragment is pushed onto the outer nav stack.
     *
     * WHY THIS IS NEEDED:
     * notesActionState is a Channel.BUFFERED with receiveAsFlow().
     * flowWithLifecycle(RESUMED) CANCELS its collector when the fragment stops (paywall opens).
     * Any NoteSaved item already in the Channel buffer is NOT consumed — it waits.
     * When the fragment resumes after paywall close, the collector restarts and immediately
     * pulls that buffered NoteSaved → checkInterstitial() → navigateScreen()
     * → navigateInnerNavToHome() → user lands on Home instead of createNotesFragment. ❌
     *
     * The flag is checked inside the NoteSaved handler (NOT in onResume, because the
     * channel item is consumed before onResume runs) and cleared after skipping.
     *
     * Stuck-flag safety:
     * If no NoteSaved is buffered (quota exceeded before save), nothing clears this flag
     * after paywall close. We use a short postDelayed in onResume to clear it after the
     * flow collector has had one cycle to process any buffered events.
     */
    var paywallCurrentlyOpen = false

    override fun onResume() {
        super.onResume()
        if (paywallCurrentlyOpen) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (paywallCurrentlyOpen) {
                    android.util.Log.d("PaywallNav", "onResume: clearing stuck paywallCurrentlyOpen flag")
                    paywallCurrentlyOpen = false
                }
            }
        }

        // Refresh the tag flexbox row whenever we return from AddTagsFragment.
        // viewModel.noteState holds the merged tag list written by handleSaveAction().
        val latestTags = viewModel.noteState.value?.tags
            ?.filter { !it.tagName.isNullOrBlank() }
            ?: emptyList()
        if (latestTags.isNotEmpty()) {
            createNoteEntity = createNoteEntity?.copy(tags = latestTags)
            latestTags.setupFlexBox()
        }
    }
    private fun showInternetPopupIfNeeded() {
        InternetConnectivityDialog.showIfNeeded(
            context             = requireContext(),
            sharedPref          = sharedPref,
            screenId            = InternetConnectivityDialog.SCREEN_CREATE_NOTE,
            isInternetConnected = internetManager.isInternetConnected
        )
    }
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
        billingManager.startConnection()  // connect early so price is ready if dialog opens
        observeNote()
        observeNoteAction()
        setupBannerObserver()
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
        val themeColor = getCurrentThemeColor(sessionManagerRepo)
        binding?.ivBottomArrow?.imageTintList = ColorStateList.valueOf(themeColor)
        setStyledDateTime(binding?.tvDate ?: return, R.color.grey)
        loadInterstitial()
        showInternetPopupIfNeeded()
        interstitialAdsConfig.loadInterstitialAd(InterAdKey.ADD_TASK_INTER_BACKPRESS)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (hasUnsavedContent()) {
                        // User typed something — show the Save as Draft dialog
                        SaveDraftDialog.show(
                            fragmentManager = childFragmentManager,

                            onSaveAsDraft = {
                                // 1. Persist the note as a draft in the DB
                                saveNoteAsDraft()
                                // 2. Then go through the normal back-press ad/navigation flow
                                proceedWithBackPress()
                            },

                            onCancel = {
                                // Do nothing — user stays on CreateNotesFragment
                            },

                            onExitAnyway = {
                                // Skip saving, go straight to ad/navigation flow
                                proceedWithBackPress()
                            }
                        )
                    } else {
                        // Nothing typed — no dialog needed, just navigate back normally
                        proceedWithBackPress()
                    }
                }
            })
    }
    private fun hasUnsavedContent(): Boolean {
        val currentTitle = binding?.etHeader?.text?.toString().orEmpty()
        val currentDesc  = binding?.etDescription?.text?.toString().orEmpty()

        val savedTitle = createNoteEntity?.title.orEmpty()
        val savedDesc  = createNoteEntity?.description.orEmpty()
        val noteId     = createNoteEntity?.noteId ?: 0L

        return when {
            // Brand-new note: show dialog only if something was typed
            noteId == 0L -> currentTitle.isNotBlank() || currentDesc.isNotBlank()

            // Editing existing note: show dialog only if text changed
            else -> currentTitle != savedTitle || currentDesc != savedDesc
        }
    }

    /**
     * Saves the current content as a DRAFT (isDraft = true) so it appears in
     * the Drafts screen.  Mirrors saveNote() but marks the note as a draft.
     *
     * ⚠️  Your CreateNoteEntity / ViewModel must support an `isDraft` flag.
     *     If it does not exist yet, add:
     *         val isDraft: Boolean = false
     *     to CreateNoteEntity and handle it in the repository / DAO layer.
     */
    /**
     * Saves current content as a draft (isDraft = true).
     *
     * Calls mergeAndSaveAsDraft() which writes to DB and emits Init — NOT NoteSaved.
     * This prevents the double-navigation race: mergeAndSave() would emit NoteSaved
     * → checkInterstitial() → navigateScreen() fires at the same time as
     * proceedWithBackPress() → navigateScreen(), crashing back to CreateNote. ❌
     * mergeAndSaveAsDraft() emits Init so proceedWithBackPress() owns navigation. ✅
     */
    private fun saveNoteAsDraft() {
        createNoteEntity = createNoteEntity?.copy(
            title       = binding?.etHeader?.text?.toString().orEmpty(),
            description = binding?.etDescription?.text?.toString().orEmpty(),
            isDraft     = true
        )
        createNoteEntity?.let {
            viewModel.mergeAndSaveAsDraft(createNoteEntity = it)
        }
    }

    /**
     * Encapsulates the original back-press flow (show interstitial or navigate).
     * Extracted so it can be called from both the dialog callbacks and the
     * no-content fast path.
     */
    private fun proceedWithBackPress() {
        if (interstitialAdsConfig.isInterstitialLoaded()) {
            showBackPressInterstitial()
        } else {
            val shouldShow = sharedPref.shouldShowRemoveAdsPopup()
            if (shouldShow) {
                proAccessManager.onInterstitialCrossClicked(
                    fragmentManager = requireActivity().supportFragmentManager,
                    onAfterDismiss  = { navigateScreen() }
                )
            } else {
                navigateScreen()
            }
        }
    }
    // Flag to prevent duplicate banner ad loading
    private var isAdLoaded = false

    private fun setupBannerObserver() {
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            if (isAdLoaded) return@observe

            val preloadedAd = adMap[BannerAdKey.ADD_TASK]

            if (preloadedAd != null) {
                isAdLoaded = true

                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()

                if (isAdded && binding != null) {
                    binding?.bannerShimmerContainer?.stopShimmer()
                    binding?.bannerShimmerContainer?.setShimmer(null)

                    binding?.bannerContainerTop?.let { container ->
                        container.setBackgroundColor(Color.TRANSPARENT)
                        container.addCleanView(preloadedAd)
                    }
                    Log.d("AdDebug", "Shimmer off, Ad visible (Loaded only once)")
                }

            } else {
                binding?.bannerShimmerContainer?.visibility = View.GONE
                Log.d("AdDebug", "ADD_TASK ad not found in map yet...")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDescriptionScroll() {
        binding?.etDescription?.setOnTouchListener { view, event ->
            if (view.hasFocus()) {
                view.parent.requestDisallowInterceptTouchEvent(true)
                if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    fun List<CustomTagEntity>.setupFlexBox() {
        val displayTags = this.filter { !it.tagName.isNullOrBlank() }

        if (displayTags.isEmpty()) {
            binding?.flexboxLayout?.visibility = View.GONE
            binding?.flexboxLayout?.removeAllViews()
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
                    val updatedTags = viewModel.removeTag(tag = tagToRemove)
                    createNoteEntity = createNoteEntity?.copy(tags = updatedTags)
                    updatedTags.setupFlexBox()
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
                showEditFeelingsDialog(
                    sessionManagerRepo = sessionManagerRepo,
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
            // ✅ FIX: Use RESUMED (not default STARTED) so this flow PAUSES while the
            // interstitial ad is showing (fragment is STOPPED) and does NOT resume/replay
            // NoteSaved when the ad dismisses and fragment comes back to STARTED.
            // With STARTED, flowWithLifecycle would re-emit the last NoteSaved value the
            // moment the fragment returns from the ad → checkInterstitial() fires again
            // on a fragment that is already mid-navigation → lands on CreateNotesFragment.
            viewModel.notesActionState.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
                .collect { note ->
                    Log.e("headerSaveSatti", "setClickListeners:$note ")
                    when (note) {
                        CreateNotesState.SaveNote -> {
                            saveNote()
                        }

                        CreateNotesState.DiscardNote -> {
                            // handle discard if needed
                        }

                        CreateNotesState.NoteSaved -> {
                            // notesActionState is a Channel.BUFFERED with receiveAsFlow().
                            // flowWithLifecycle(RESUMED) CANCELS the collector when the fragment
                            // stops (paywall opens) and RESTARTS it on resume. Any NoteSaved
                            // sitting in the Channel buffer is consumed immediately on resume —
                            // BEFORE onResume() runs. So the guard must live HERE, not onResume.
                            //
                            // Sequence when paywall ✕:
                            //   flowWithLifecycle restarts → pulls NoteSaved from buffer
                            //   → paywallCurrentlyOpen is still true → skip navigation
                            //   → set paywallCurrentlyOpen = false for the next real save
                            if (paywallCurrentlyOpen) {
                                paywallCurrentlyOpen = false
                                return@collect
                            }
                            checkInterstitial()
                        }

                        is CreateNotesState.ShowMessage -> {
                            // handle message if needed
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
                                        any { it.tagName.orEmpty().equals("", ignoreCase = true) }
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
                                createNoteEntity?.copy(
                                    backgroundRes = note.bgImageRes,
                                    bgImageUri = null  // drawable bg clears any gallery URI
                                )
                        }

                        is CreateNotesState.ChangeBgUri -> {
                            createNoteEntity =
                                createNoteEntity?.copy(
                                    bgImageUri = note.bgImageUri,
                                    backgroundRes = null  // gallery bg clears any drawable res
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

    fun getTitleText(): String {
        return binding?.etHeader?.text?.toString().orEmpty()
    }

    /** Called by MainFragment's kabab-menu bottom sheet → Share option. */
    fun shareNote() {
        val note = createNoteEntity ?: viewModel.noteState.value ?: return
        val shareText = buildString {
            if (!note.title.isNullOrBlank()) appendLine(note.title)
            if (!note.text.isNullOrBlank())  append(note.text)
        }.trim()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, note.title.orEmpty())
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }

    /** Called by MainFragment's kabab-menu bottom sheet → Delete option. */
    fun deleteNote() {
        val note = createNoteEntity ?: viewModel.noteState.value ?: return
        com.example.easydiarysatti.utills.ConfirmationDialog.showDelete(
            fm        = childFragmentManager,
            count     = 1,
            onConfirm = {
                homeViewModel.deleteNote(note)
                proceedWithBackPress()
            },
            onCancel  = {}
        )
    }

    /**
     * Called by MainFragment just before the paywall opens.
     * Snapshots the current title + description from the EditTexts into the ViewModel's
     * noteState so the data survives CreateNotesFragment being destroyed and recreated
     * when MainFragment's view is torn down while MainPaywallFragment is on screen.
     * When CreateNotesFragment is recreated, observeNote() → setupDefaultValues()
     * restores everything from the ViewModel automatically.
     */
    fun snapshotDraftToViewModel() {
        val title = binding?.etHeader?.text?.toString().orEmpty()
        val description = binding?.etDescription?.text?.toString().orEmpty()
        createNoteEntity = createNoteEntity?.copy(title = title, description = description)
        createNoteEntity?.let {
            viewModel.setupNoteEntity(it)
            android.util.Log.d("PaywallNav", "snapshotDraftToViewModel: title='$title' desc='${description.take(30)}'")
        }
    }

    private fun saveNote() {
        createNoteEntity = createNoteEntity?.copy(
            title = binding?.etHeader?.text?.toString().orEmpty(),
            description = binding?.etDescription?.text?.toString().orEmpty()
        )

        createNoteEntity?.let {
            viewModel.mergeAndSave(createNoteEntity = it)
        } ?: run {
            Log.e("headerSaveSatti", "setClickListeners:$createNoteEntity is Null")
            return
        }

        // ✅ FIX: Reset the action state immediately after triggering save.
        // Because viewModel is activityViewModels() (shared), the Channel retains the
        // last SaveNote event. Without this reset, re-opening CreateNotesFragment a 2nd
        // time causes observeNoteAction() to replay SaveNote → fires saveNote() again →
        // shows the ad → cross navigates back to CreateNote instead of Home.
        viewModel.resetActionState()

        // NOTE: checkInterstitial() is NO LONGER called here.
        // It is now triggered from observeNoteAction() when CreateNotesState.NoteSaved
        // is emitted by the ViewModel — i.e. only AFTER the DB write has completed.
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
        viewModel.addTags(
            tags = createNoteEntity?.tags?.filter { !it.tagName.isNullOrBlank() } ?: listOf()
        )
        // Restore gallery background if this note was saved with one
        createNoteEntity?.bgImageUri?.let { uriString ->
            viewModel.sendAction(CreateNotesState.ChangeBgUri(bgImageUri = uriString))
        } ?: createNoteEntity?.backgroundRes?.let { resId ->
            viewModel.sendAction(CreateNotesState.ChangeBg(bgImageRes = resId))
        }
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
                etHeader.setHeadingSize(textSizeInSp = it)
            }
            fontSizePair?.second?.let {
                etDescription.setHeadingSize(textSizeInSp = it)
            }
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etHeader, it) }
            createNoteEntity?.textAlignment?.let { setTextAlignmentByName(etDescription, it) }
            createNoteEntity?.textFont?.let { etHeader.setFont(it, context ?: return) }
            createNoteEntity?.textFont?.let { etDescription.setFont(it, context ?: return) }
        }
    }

    fun addDefaultTags() {
        val tags = createNoteEntity?.tags?.toMutableList() ?: mutableListOf()
        val validTags = tags.filter { !it.tagName.isNullOrBlank() }

        viewModel.clearTags()
        if (validTags.isNotEmpty()) {
            viewModel.addTags(validTags)
        }

        createNoteEntity = createNoteEntity?.copy(tags = validTags)

        if (validTags.isNotEmpty()) {
            binding?.flexboxLayout?.visibility = View.VISIBLE
            validTags.setupFlexBox()
        } else {
            binding?.flexboxLayout?.visibility = View.GONE
        }
    }

    private fun loadInterstitial() {
        interstitialAdsConfig.loadInterstitialAd(InterAdKey.ADD_NOTE_INTER_SAVE_BUTTON)
    }

    private fun checkInterstitial() {
        if (sessionManagerRepo.wasRewardedJustShown()) {
            navigateScreen()
            return
        }
        when (interstitialAdsConfig.isInterstitialLoaded()) {
            true -> showInterstitial()
            false -> navigateScreen()
        }
    }

    private fun showBackPressInterstitial() {
        sessionManagerRepo.bypassSecurityLogin(true)
        // Reset guard so the cross/dismiss callback can always navigate
        hasNavigated = false

        interstitialAdsConfig.showInterstitialWithDialog(
            requireActivity(),
            InterAdKey.ADD_TASK_INTER_BACKPRESS,
            object : InterstitialOnShowCallBack {

                // After the back-press interstitial is dismissed/failed, check whether
                // the Remove Ads popup counter has been reached (per remote config
                // remove_ads_inter_ad_cross_ipu). If yes, show the dialog; if no, navigate.
                override fun onAdDismissedFullScreenContent() {
                    checkRemoveAdsPopupOrNavigate()
                }

                override fun onAdFailedToShow() {
                    checkRemoveAdsPopupOrNavigate()
                }

                override fun onAdImpressionDelayed() {
                    checkRemoveAdsPopupOrNavigate()
                }
            }
        )
    }

    /**
     * Single place that checks the Remove Ads popup counter and either
     * shows RemoveAdsDialog (no navigation) or navigates up.
     * Called from every dismiss/fail/delay path of both interstitials.
     */
    private fun checkRemoveAdsPopupOrNavigate() {
        val shouldShow = sharedPref.shouldShowRemoveAdsPopup()
        if (shouldShow) {
            proAccessManager.onInterstitialCrossClicked(
                fragmentManager = requireActivity().supportFragmentManager,
                onAfterDismiss = { navigateScreen() }
            )
        } else {
            navigateScreen()
        }
    }

    private fun showInterstitial() {
        sessionManagerRepo.bypassSecurityLogin(true)

        // Navigate to Home BEFORE showing the interstitial so the back stack is already
        // clean while the ad plays. When the ad dismisses, Home is already the active
        // destination — the user sees a seamless transition with zero flash of CreateNote.
        navigateScreen()

        interstitialAdsConfig.showInterstitialWithDialog(
            requireActivity(),
            InterAdKey.ADD_NOTE_INTER_SAVE_BUTTON,
            object : InterstitialOnShowCallBack {

                override fun onAdDismissedFullScreenContent() {
                    // Navigation already done — only check the Remove Ads popup counter.
                    checkRemoveAdsPopupOnly()
                }

                override fun onAdFailedToShow() {
                    checkRemoveAdsPopupOnly()
                }

                override fun onAdImpressionDelayed() {
                    checkRemoveAdsPopupOnly()
                }
            })
    }

    /**
     * Checks the Remove Ads popup counter WITHOUT navigating.
     * Used by showInterstitial() where navigation has already happened before the ad shows.
     */
    private fun checkRemoveAdsPopupOnly() {
        val shouldShow = sharedPref.shouldShowRemoveAdsPopup()
        if (shouldShow) {
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                proAccessManager.onInterstitialCrossClicked(
                    fragmentManager = requireActivity().supportFragmentManager,
                    onAfterDismiss  = { /* navigation already done, nothing to do */ }
                )
            }
        }
    }

    override fun onDestroyView() {
        billingManager.endConnection()
        super.onDestroyView()
    }

    /**
     * Navigates the user to the Home screen after the interstitial ad is dismissed.
     *
     * WHY THIS IS NEEDED:
     * CreateNotesFragment lives inside a nested NavHostFragment (inner nav).
     * Calling findNavController().navigateUp() from here resolves to the INNER nav controller,
     * which pops back to CreateNotesFragment instead of going to Home.
     *
     * STRATEGY (3 levels, most-reliable first):
     * 1. Walk the fragment back-stack to find MainFragment and call navigateInnerNavToHome()
     *    directly — this is the cleanest path and handles all nested nav cases.
     * 2. If MainFragment isn't found via nav_host_container, search ALL fragments in the
     *    activity's supportFragmentManager recursively — handles edge cases where the
     *    container ID differs or the fragment is nested deeper.
     * 3. Last resort: pop to the start destination of the current nav graph. This is still
     *    better than navigateUp() because it always goes to root, not just one step back.
     */
    private fun navigateScreen() {
        // Guard: fire at most once per fragment instance
        if (hasNavigated) return
        hasNavigated = true

        // Read and immediately clear the "opened from draft" flag so it doesn't
        // persist across subsequent CreateNote sessions.
        val fromDraft = viewModel.openedFromDraft
        viewModel.openedFromDraft = false

        val hostActivity = activity ?: return

        Log.d("navigateScreen", "navigateScreen() called — fromDraft=$fromDraft")

        fun doNavigate() {
            if (hostActivity.isFinishing || hostActivity.isDestroyed) {
                Log.e("navigateScreen", "Activity is finishing/destroyed — aborting")
                hasNavigated = false
                return
            }

            val mainFragment =
                // Strategy 1: find MainFragment via the primary nav host container
                (hostActivity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_container)
                    ?.childFragmentManager
                    ?.fragments
                    ?.firstOrNull { it is com.example.easydiarysatti.ui.main.MainFragment }
                        as? com.example.easydiarysatti.ui.main.MainFragment)
                // Strategy 2: search all fragments in the activity recursively
                    ?: findMainFragmentRecursive(hostActivity.supportFragmentManager)

            if (mainFragment != null) {
                if (fromDraft) {
                    // Opened from DraftNotesFragment — go back to Drafts screen
                    Log.d("navigateScreen", "fromDraft=true → navigateBackToDraft()")
                    mainFragment.navigateBackToDraft()
                } else {
                    // Normal path — go back to Home
                    Log.d("navigateScreen", "fromDraft=false → navigateInnerNavToHome()")
                    mainFragment.navigateInnerNavToHome()
                }
                return
            }

            // Strategy 3: last resort — pop inner nav
            try {
                val navController = findNavController()
                val popped = navController.popBackStack(navController.graph.startDestinationId, false)
                Log.d("navigateScreen", "Strategy 3 popBackStack result: $popped")
                if (!popped) navController.navigateUp()
            } catch (e: Exception) {
                Log.e("navigateScreen", "Strategy 3 failed: ${e.message}")
            }
        }

        // On Android 11, onAdDismissedFullScreenContent fires while FM isStateSaved=true.
        // Handler.post() is still too early — the runnable executes before Activity.onStart()
        // clears saved state and FM still rejects popBackStack().
        // Solution: observe the lifecycle and navigate on the STARTED event, which fires
        // exactly when onStateNotSaved() is called and FM accepts transactions again.
        if (hostActivity.supportFragmentManager.isStateSaved) {
            Log.d("navigateScreen", "FM state saved — waiting for STARTED lifecycle")
            viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    Log.d("navigateScreen", "STARTED — retrying navigation")
                    doNavigate()
                }
            })
        } else {
            doNavigate()
        }
    }

    /**
     * Recursively searches all fragment managers to find a MainFragment instance.
     * Handles deeply nested NavHostFragment structures.
     */
    private fun findMainFragmentRecursive(
        fragmentManager: androidx.fragment.app.FragmentManager
    ): com.example.easydiarysatti.ui.main.MainFragment? {
        for (fragment in fragmentManager.fragments) {
            if (fragment is com.example.easydiarysatti.ui.main.MainFragment) return fragment
            val found = findMainFragmentRecursive(fragment.childFragmentManager)
            if (found != null) return found
        }
        return null
    }

    fun onClickDateTimePick() {
        (activity as? MainActivity)?.requestExactAlarmPermission {
            showDatePickerWithTime(
                sessionManagerRepo = sessionManagerRepo,
                calendar = Calendar.getInstance()
            ) { selectedCalendar ->
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
}
