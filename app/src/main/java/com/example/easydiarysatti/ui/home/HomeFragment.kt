package com.example.easydiarysatti.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentHomeBinding
import com.example.easydiarysatti.monthlyFormatDate
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.setStyledDateTime
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.createnote.NotesItemAdapter
import com.example.easydiarysatti.ui.main.MainState
import com.example.easydiarysatti.ui.main.MainViewModel
import com.example.easydiarysatti.viewBinding
import com.example.easydiarysatti.visible
import com.google.android.gms.ads.AdView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.delay

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val viewModel by viewModels<HomeViewModel>()
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val viewModelBanner by viewModels<ViewModelBanner>()
    private var reviewTriggered = false
    private lateinit var swipeHandler: ItemTouchHelper.SimpleCallback

    private val notesItemAdapter: NotesItemAdapter by lazy {
        NotesItemAdapter(
            onNoteItemClick = { note ->
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                createNotesViewModel.setupNoteEntity(createNoteEntity = note)
                moveToNextScreen()
            },
            onNoteItemLongClick = { note ->
                findNavController().safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId = R.id.action_homeFragment_to_previewFragment2,
                    bundle = Bundle().apply {
                        putLong(NOTE_ID, note.noteId)
                        putBoolean(FROM_SCREEN, true)
                    }
                )
            },
            onFavClick = { note ->

                viewModel.toggleFavorite(note)
                // Optional: Show feedback
            },
            onDeleteClick = { note ->
                viewModel.deleteNote(note)
            }
        )
    }

    private fun setupSwipeActions() {

        swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val note = notesItemAdapter.currentList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // DELETE
                    logAnalyticsEvent("Home_Delete_Note", "delete_click")
                    viewModel.deleteNote(note)

                } else {
                    logAnalyticsEvent("Home_Favourite_Note", "swipe_toggle_fav")
                    // FAVORITE (snap back)
                    viewModel.toggleFavorite(note)

                    // 🔥 FORCE SNAP BACK
                    swipeHandler.clearView(binding!!.rvNotes, viewHolder)

                    viewHolder.itemView.animate()
                        .translationX(0f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                drawSwipeBackground(c, viewHolder, dX)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding?.rvNotes)
    }


    private fun drawSwipeBackground(c: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.delete_ic)
        val favIcon = ContextCompat.getDrawable(requireContext(), R.drawable.fav_ic)

        val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0

        if (dX > 0) { // Right Swipe (Favorite)
            val background = ColorDrawable(Color.parseColor("#E8BA00"))
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
            background.draw(c)

            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconLeft = itemView.left + 60
            favIcon?.setBounds(iconLeft, iconTop, iconLeft + intrinsicWidth, iconTop + intrinsicHeight)
            favIcon?.draw(c)

        } else if (dX < 0) { // Left Swipe (Delete)
            val background = ColorDrawable(Color.parseColor("#E80200"))
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            background.draw(c)

            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconRight = itemView.right - 60
            deleteIcon?.setBounds(iconRight - intrinsicWidth, iconTop, iconRight, iconTop + intrinsicHeight)
            deleteIcon?.draw(c)
        }
    }
    abstract class SwipeActionCallback(context: Context) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

        private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.delete_ic)
        private val favIcon = ContextCompat.getDrawable(context, R.drawable.fav_ic)
        private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
        private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0

        // Background colors
        private val deleteBackground = ColorDrawable(Color.parseColor("#EF4444")) // Red
        private val favBackground = ColorDrawable(Color.parseColor("#E80200"))    // Amber

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

        override fun onChildDraw(
            c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top

            if (dX > 0) { // Swiping Right (Favorite)
                favBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                favBackground.draw(c)

                // Draw Fav Icon
                val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val iconLeft = itemView.left + 40
                val iconRight = itemView.left + 40 + intrinsicWidth
                val iconBottom = iconTop + intrinsicHeight
                favIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                favIcon?.draw(c)

            } else if (dX < 0) { // Swiping Left (Delete)
                deleteBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                deleteBackground.draw(c)

                // Draw Delete Icon
                val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val iconRight = itemView.right - 40
                val iconLeft = itemView.right - 40 - intrinsicWidth
                val iconBottom = iconTop + intrinsicHeight
                deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                deleteIcon?.draw(c)
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun onResume() {
        super.onResume()
        mainViewModel.setMainState(MainState.HomeScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val eventParams = Bundle()
        eventParams.putString("HomeScreen", "open_screen")
        mFirebaseAnalytics.logEvent("Home_Screen", eventParams)
        clickListener()
        setupRecyclerView()
        observeAllNotes()
        setupTodayDate()
        observeSortOrder()
        setupSwipeActions()
        setStyledDateTime(binding?.tvDate ?: return, R.color.track_color)

        loadBanner()
        initObservers()

    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply {
            putString("action_label", label)
        }
        mFirebaseAnalytics.logEvent(eventName, params)
    }
    private fun clickListener() {
        binding?.apply {
            ivSorting.setOnClickListener {
                viewModel.updateSortOrder()
            }
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

                            // 🔥 Show review only once & only when attached
                            if (!state.notes.isNullOrEmpty() && !reviewTriggered) {
                                reviewTriggered = true

                                viewLifecycleOwner.lifecycleScope.launch {
                                    delay(1500)

                                    // SAFETY CHECK
                                    if (isAdded && activity != null) {
                                        launchInAppReview(requireActivity())
                                    }
                                }
                            }

                            if (state.notes.isNullOrEmpty()) return@collect

                            if (viewModel.currentSortOrder) {
                                binding?.rvNotes?.smoothScrollToPosition(
                                    state.notes.size - 1
                                )
                            } else {
                                binding?.rvNotes?.smoothScrollToPosition(0)
                            }
                        }

                        is HomeNotesState.Error -> {
                            binding?.visible(hasNotes = false)
                        }

                        else -> Unit
                    }
                }
        }
    }

    fun launchInAppReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We can get the ReviewInfo object
                val reviewInfo = task.result

                // Launch the in-app review flow
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // The review flow has finished, but the user may not have left a review
                    Log.d("Review", "Review flow complete")
                }
            } else {
                // Fallback option (e.g., open Play Store page)
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
        binding?.tvDate?.apply {
            val currentTimestamp = System.currentTimeMillis()
            val formattedDate = context?.monthlyFormatDate(currentTimestamp)
            text = formattedDate
        }
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


    private fun loadBanner() {
        context?.let {
            val adView = AdView(it)
            viewModelBanner.loadBannerAd(adView, BannerAdKey.HOME, context ?: return@let)
        }
    }

    private fun initObservers() {
        viewModelBanner.adViewLiveData.observe(viewLifecycleOwner) {
            binding?.bannerAdViewHome?.addCleanView(it)
        }
        viewModelBanner.loadFailedLiveData.observe(viewLifecycleOwner) {
            binding?.bannerAdViewHome?.visibility = View.GONE
        }
        viewModelBanner.clearViewLiveData.observe(viewLifecycleOwner) {
            binding?.bannerAdViewHome?.removeAllViews()
        }
    }


}