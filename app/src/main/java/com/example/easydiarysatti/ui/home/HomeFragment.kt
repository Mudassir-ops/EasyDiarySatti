package com.example.easydiarysatti.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.appOpen.screen.AppOpenAdsConfig
import com.example.easydiarysatti.ads.appOpen.screen.enums.AppOpenAdKey
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.monthlyFormatDate
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.createnote.NotesItemAdapter
import com.example.easydiarysatti.ui.main.MainState
import com.example.easydiarysatti.ui.main.MainViewModel
import com.example.easydiarysatti.utills.ConfirmationDialog
import com.example.easydiarysatti.utills.InternetConnectivityDialog
import com.example.easydiarysatti.viewBinding
import com.example.easydiarysatti.visible
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    private val viewModelNative by viewModels<ViewModelNative>()
    private var reviewTriggered = false

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var appOpenAdsConfig: AppOpenAdsConfig
    @Inject lateinit var internetManager: InternetManager

    private lateinit var swipeHandler: ItemTouchHelper.SimpleCallback

    // ── Multi-select state ───────────────────────────────────────────────────
    var isSelectionMode = false
        private set

    private val selectedNoteIds = mutableSetOf<Long>()

    /**
     * Set by MainFragment after this fragment is created.
     * Called whenever selection mode is entered/exited OR the count changes.
     *   isActive  = true  → selection mode ON  (show iv_delete_all in toolbar)
     *   isActive  = false → selection mode OFF (hide iv_delete_all)
     *   count             → number of currently selected notes (for display)
     */
    var onSelectionChanged: ((isActive: Boolean, count: Int) -> Unit)? = null

    // ── Adapter ──────────────────────────────────────────────────────────────
    private val notesItemAdapter: NotesItemAdapter by lazy {
        NotesItemAdapter(
            onNoteItemClick = { note ->
                if (isSelectionMode) {
                    toggleNoteSelection(note.noteId)
                } else {
                    createNotesViewModel.clearTags()
                    createNotesViewModel.clearImages()
                    createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                    createNotesViewModel.setupNoteEntity(createNoteEntity = note)
                    moveToNextScreen()
                }
            },
            onNoteItemLongClick = { note ->
                if (!isSelectionMode) {
                    enterSelectionMode(note.noteId)
                } else {
                    findNavController().safeNav(
                        currentDestId = R.id.homeFragment,
                        actionId = R.id.action_homeFragment_to_previewFragment2,
                        bundle = Bundle().apply {
                            putLong(NOTE_ID, note.noteId)
                            putBoolean(FROM_SCREEN, true)
                        }
                    )
                }
            },
            onFavClick = { note -> viewModel.toggleFavorite(note) },
            onDeleteClick = { note -> viewModel.deleteNote(note) },
                    onMoreOptionClick = { view, note ->
                showPopupMenu(view, note)
            }
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Multi-select helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun enterSelectionMode(firstNoteId: Long) {
        isSelectionMode = true
        selectedNoteIds.clear()
        selectedNoteIds.add(firstNoteId)
        notesItemAdapter.setSelectedIds(selectedNoteIds)
        // Notify MainFragment: show delete icon in its toolbar
        onSelectionChanged?.invoke(true, selectedNoteIds.size)
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedNoteIds.clear()
        notesItemAdapter.setSelectedIds(emptySet())
        // Notify MainFragment: hide delete icon
        onSelectionChanged?.invoke(false, 0)
    }

    private fun toggleNoteSelection(noteId: Long) {
        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.remove(noteId)
        } else {
            selectedNoteIds.add(noteId)
        }

        if (selectedNoteIds.isEmpty()) {
            exitSelectionMode()
            return
        }

        notesItemAdapter.setSelectedIds(selectedNoteIds)
        // Notify MainFragment: update count
        onSelectionChanged?.invoke(true, selectedNoteIds.size)
    }
    private fun showPopupMenu(view: View, note: CreateNoteEntity) {
        val wrapper = androidx.appcompat.view.ContextThemeWrapper(
            requireContext(),
            R.style.PopupMenuStyle          // ← controls background color
        )
        val popup = androidx.appcompat.widget.PopupMenu(wrapper, view, android.view.Gravity.END)

        popup.menuInflater.inflate(R.menu.note_item_menu, popup.menu)

        // Force icons to show — PopupMenu hides them by default
        try {
            val fieldPopup = androidx.appcompat.widget.PopupMenu::class.java
                .getDeclaredField("mPopup")
            fieldPopup.isAccessible = true
            val menuPopupHelper = fieldPopup.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper
                .getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.menu_add_tag -> {
                    logAnalyticsEvent("Note_Item_Menu_AddTag", "popup_menu")
                    navigateToNote(note, openTags = true)
                    true
                }

                R.id.menu_duplicate -> {
                    logAnalyticsEvent("Note_Item_Menu_Duplicate", "popup_menu")
                    viewModel.duplicateNote(note)
                    true
                }

                R.id.menu_edit -> {
                    logAnalyticsEvent("Note_Item_Menu_Edit", "popup_menu")
                    navigateToNote(note, openTags = false)
                    true
                }

                R.id.menu_share -> {
                    logAnalyticsEvent("Note_Item_Menu_Share", "popup_menu")
                    shareNoteText(note)
                    true
                }

                R.id.menu_delete -> {
                    logAnalyticsEvent("Note_Item_Menu_Delete", "popup_menu")
                    ConfirmationDialog.showDelete(
                        fm        = childFragmentManager,
                        count     = 1,
                        onConfirm = { viewModel.deleteNote(note) }
                    )
                    true
                }

                else -> false
            }
        }
        popup.show()
    }
    /**
     * Called by MainFragment when the user taps iv_delete_all in the header.
     * Shows a confirmation dialog then bulk-deletes all selected notes.
     */
    fun deleteSelectedNotes() {
        val count = selectedNoteIds.size
        if (count == 0) return

        ConfirmationDialog.showDelete(
            fm        = childFragmentManager,
            count     = count,
            onConfirm = {
                val notesToDelete = notesItemAdapter.currentList
                    .filter { selectedNoteIds.contains(it.noteId) }
                notesToDelete.forEach { viewModel.deleteNote(it) }
                exitSelectionMode()
                logAnalyticsEvent("Home_Bulk_Delete", "multi_delete")
            },
            onCancel  = {
                // Cancel or dismiss without confirming: deselect all notes
                exitSelectionMode()
            }
        )
    }


    private fun navigateToNote(note: CreateNoteEntity, openTags: Boolean) {
        createNotesViewModel.setupNoteEntity(note)
        findNavController().navigate(
            R.id.action_homeFragment_to_createNotesFragment2,
            Bundle().apply {
                putLong(com.example.easydiarysatti.NOTE_ID, note.noteId)
                putBoolean("OPEN_TAGS_DIRECTLY", openTags)
            }
        )
    }

    private fun shareNoteText(note: CreateNoteEntity) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.text}")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Note via"))
    }
    // ────────────────────────────────────────────────────────────────────────
    //  Swipe actions (with confirmation dialogs)
    // ────────────────────────────────────────────────────────────────────────

    private fun setupSwipeActions() {
        swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (isSelectionMode) return 0
                if (viewHolder is NotesItemAdapter.AdViewHolder) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition

                if (position == RecyclerView.NO_POSITION ||
                    viewHolder is NotesItemAdapter.AdViewHolder
                ) {
                    notesItemAdapter.notifyItemChanged(position)
                    return
                }

                val adLoaded =
                    viewModelNative.adMapLiveData.value?.containsKey(NativeAdKey.HOME) == true
                val noteIndex = if (adLoaded && position > 1) position - 1 else position

                if (noteIndex < 0 || noteIndex >= notesItemAdapter.currentList.size) {
                    notesItemAdapter.notifyItemChanged(position)
                    return
                }

                val note = notesItemAdapter.currentList[noteIndex]
                // Snap item back — dialog handles the action
                notesItemAdapter.notifyItemChanged(position)
                if (direction == ItemTouchHelper.LEFT) {
                    logAnalyticsEvent("Home_Delete_Note_Swipe", "swipe_delete")
                    ConfirmationDialog.showDelete(
                        fm = childFragmentManager,
                        count = 1,
                        onConfirm = {
                            viewModel.deleteNote(note)
                            logAnalyticsEvent("Home_Delete_Note", "delete_confirm")
                        }
                        // No onCancel needed for single-swipe delete
                    )
                } else {
                    logAnalyticsEvent("Home_Favourite_Note_Swipe", "swipe_fav")
                    ConfirmationDialog.showFavorite(
                        fm = childFragmentManager,
                        isFav = note.isFavorite,
                        onConfirm = {
                            viewModel.toggleFavorite(note)
                            logAnalyticsEvent("Home_Favourite_Note", "fav_confirm")
                        }
                    )
                }


            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder !is NotesItemAdapter.AdViewHolder) {
                    drawSwipeBackground(c, viewHolder, dX)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding?.rvNotes)
    }

    private fun drawSwipeBackground(c: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_ic)
        val favIcon    = ContextCompat.getDrawable(requireContext(), R.drawable.fav_ic)
        val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        val intrinsicWidth  = deleteIcon?.intrinsicWidth  ?: 0

        if (dX > 0) {
            val background = ColorDrawable(Color.parseColor("#E8BA00"))
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            background.draw(c)
            val iconTop  = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconLeft = itemView.left + 60
            favIcon?.setBounds(iconLeft, iconTop, iconLeft + intrinsicWidth, iconTop + intrinsicHeight)
            favIcon?.draw(c)
        } else if (dX < 0) {
            val background = ColorDrawable(Color.parseColor("#E80200"))
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)
            val iconTop   = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconRight = itemView.right - 60
            deleteIcon?.setBounds(iconRight - intrinsicWidth, iconTop, iconRight, iconTop + intrinsicHeight)
            deleteIcon?.draw(c)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        mainViewModel.setMainState(MainState.HomeScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        mFirebaseAnalytics.logEvent("Home_Screen", Bundle().apply {
            putString("HomeScreen", "open_screen")
        })

        clickListener()
        setupRecyclerView()
        observeAllNotes()
        setupTodayDate()
        observeSortOrder()
        setupSwipeActions()

        setStyledDateTime(binding?.tvDate ?: return, R.color.track_color)

        appOpenAdsConfig.loadAppOpenAd(AppOpenAdKey.RESUME)
        viewModelNative.loadNativeAd(NativeAdKey.HOME)
        initNativeObserver()

        val adKey = if (sharedPref.isFirstTimeUser)
            BannerAdKey.HOME_FIRST_TIME else BannerAdKey.HOME_RETURNING
        val dummyAdView = com.google.android.gms.ads.AdView(requireContext())
        bannerViewModel.loadBannerAd(dummyAdView, adKey, requireContext())
        initBannerObserver()
        showInternetPopupIfNeeded()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isAdProcessStarted = false
        if (isSelectionMode) exitSelectionMode()
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Existing helpers (unchanged)
    // ────────────────────────────────────────────────────────────────────────

    private fun showInternetPopupIfNeeded() {
        InternetConnectivityDialog.showIfNeeded(
            context            = requireContext(),
            sharedPref         = sharedPref,
            screenId           = InternetConnectivityDialog.SCREEN_HOME,
            isInternetConnected = internetManager.isInternetConnected
        )
    }

    fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        mFirebaseAnalytics.logEvent(eventName, Bundle().apply {
            putString("action_label", label)
        })
    }

    private fun clickListener() {
        binding?.apply {
            ivSorting.setOnClickListener { viewModel.updateSortOrder() }
        }
    }

    private fun moveToNextScreen() {
        findNavController().safeNav(
            currentDestId = R.id.homeFragment,
            actionId = R.id.action_homeFragment_to_createNotesFragment2
        )
    }

    private fun setupRecyclerView() {
        binding?.rvNotes?.run {
            adapter = notesItemAdapter
            hasFixedSize()
        }
    }

    private fun observeAllNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allNotesState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is HomeNotesState.Success -> {
                            binding?.visible(hasNotes = true)
                            notesItemAdapter.submitList(state.notes)

                            if (!state.notes.isNullOrEmpty() && !reviewTriggered) {
                                reviewTriggered = true
                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(1500)
                                    if (isAdded && activity != null) launchInAppReview(requireActivity())
                                }
                            }

                            if (state.notes.isNullOrEmpty()) return@collect
                            if (viewModel.currentSortOrder)
                                binding?.rvNotes?.smoothScrollToPosition(state.notes.size - 1)
                            else
                                binding?.rvNotes?.smoothScrollToPosition(0)
                        }
                        is HomeNotesState.Error -> binding?.visible(hasNotes = false)
                        else -> Unit
                    }
                }
        }
    }

    fun launchInAppReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener { Log.d("Review", "Review flow complete") }
            } else {
                openPlayStore(activity)
            }
        }
    }

    fun openPlayStore(context: Context) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTodayDate() {
        binding?.tvDate?.text = context?.monthlyFormatDate(System.currentTimeMillis())
    }

    private fun observeSortOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortOrder
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { isAscending ->
                    viewModel.currentSortOrder = isAscending == true
                    binding?.ivSorting?.animate()
                        ?.rotation(if (isAscending == true) 0f else 180f)
                        ?.setDuration(100)
                        ?.start()
                }
        }
    }

    private var isAdProcessStarted = false

    private fun initBannerObserver() {
        if (!sharedPref.getAdShowStatus(BannerAdKey.START_WRITING.value)) {
            binding?.shimmerViewContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            binding?.bannerAdViewHome?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            if (isAdProcessStarted) return@observe
            val homeAd = adMap[BannerAdKey.HOME_FIRST_TIME] ?: adMap[BannerAdKey.HOME_RETURNING]
            if (homeAd != null) {
                isAdProcessStarted = true
                binding?.shimmerViewContainer?.apply { stopShimmer(); visibility = View.GONE }
                binding?.bannerAdViewHome?.apply {
                    visibility = View.VISIBLE
                    removeAllViews()
                    addCleanView(homeAd)
                }
            } else {
                binding?.shimmerViewContainer?.visibility = View.VISIBLE
                binding?.bannerAdViewHome?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
            }
        }
    }

    private fun initNativeObserver() {
        viewModelNative.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            val homeNativeAd = adMap[NativeAdKey.HOME]
            if (homeNativeAd != null) {
                notesItemAdapter.setNativeAd(homeNativeAd)
                Log.d("AdDebug", "Home Native Ad updated from Map")
            }
        }
    }

    abstract class SwipeActionCallback(context: Context) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private val deleteIcon  = ContextCompat.getDrawable(context, R.drawable.delete_ic)
        private val favIcon     = ContextCompat.getDrawable(context, R.drawable.fav_ic)
        private val intrinsicWidth  = deleteIcon?.intrinsicWidth  ?: 0
        private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        private val deleteBackground = ColorDrawable(Color.parseColor("#EF4444"))
        private val favBackground    = ColorDrawable(Color.parseColor("#E80200"))
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
        override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
            val iv = vh.itemView; val h = iv.bottom - iv.top
            if (dX > 0) {
                favBackground.setBounds(iv.left, iv.top, iv.left + dX.toInt(), iv.bottom); favBackground.draw(c)
                val top = iv.top + (h - intrinsicHeight) / 2
                favIcon?.setBounds(iv.left + 40, top, iv.left + 40 + intrinsicWidth, top + intrinsicHeight); favIcon?.draw(c)
            } else if (dX < 0) {
                deleteBackground.setBounds(iv.right + dX.toInt(), iv.top, iv.right, iv.bottom); deleteBackground.draw(c)
                val top = iv.top + (h - intrinsicHeight) / 2
                deleteIcon?.setBounds(iv.right - 40 - intrinsicWidth, top, iv.right - 40, top + intrinsicHeight); deleteIcon?.draw(c)
            }
            super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
        }
    }
}