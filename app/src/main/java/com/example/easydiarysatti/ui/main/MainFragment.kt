package com.example.easydiarysatti.ui.main

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.MainActivity
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.rewarded.RewardedAdsConfig
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.rewarded.enums.RewardedAdKey
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.privacyPolicyUrl
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.utills.ImagePickerDelegate
import com.example.easydiarysatti.utills.MultiImageAdapter
import com.example.easydiarysatti.utills.pickPhotDialog
import com.example.easydiarysatti.utills.setImage
import com.example.easydiarysatti.utills.showBackgroundDialog
import com.example.easydiarysatti.utills.showEditTexDialog
import com.example.easydiarysatti.utills.showFeedBackDialog
import com.example.easydiarysatti.utills.showImageCropDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.LinkedList
import javax.inject.Inject
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.ui.edittags.AddTagsFragment
import com.example.easydiarysatti.utills.getCurrentThemeColor
import com.google.firebase.analytics.FirebaseAnalytics

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var calendarHost: NavHostFragment
    private lateinit var libraryHost: NavHostFragment
    private lateinit var homeHost: NavHostFragment
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private val viewModelCreateNote: CreateNotesViewModel by activityViewModels()
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val viewModel by activityViewModels<NameViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate
    private var activeNavHost: NavHostFragment? = null

    // Backstack for Bottom Navigation IDs to maintain history
    private val backStack = LinkedList<Int>()

    private val navHostListeners =
        mutableMapOf<NavHostFragment, NavController.OnDestinationChangedListener>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    @Inject
    lateinit var rewardedAdsConfig: RewardedAdsConfig

    private val multiImageAdapter: MultiImageAdapter by lazy {
        MultiImageAdapter(
            items = (activity as MainActivity).getBgThemes(),
            onUploadClick = {},
            onImageClick = {
                if (view != null) {
                    binding?.ivCreateNote?.loadImage(
                        resourceId = it, placeholder = 0
                    )
                }
                createNotesViewModel.sendAction(
                    CreateNotesState.ChangeBg(
                        bgImageRes = it ?: return@MultiImageAdapter
                    )
                )
            })
    }

    private val drawerItemAdapter: DrawerItemAdapter by lazy {
        DrawerItemAdapter(onNoteItemClick = {
            when (it) {
                0 -> {
                    logAnalyticsEvent("Drawer_Edit_Tags", "drawer_click")
                    findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_addTagsFragment2,
                    bundle = Bundle().apply {
                        putBoolean(FROM_SCREEN, true)
                    })}

                1 -> {
                    logAnalyticsEvent("Drawer_Color_Theme", "drawer_click")
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId = R.id.action_mainFragment_to_themesFragment
                    )
                }
                2 -> {
                    logAnalyticsEvent("Drawer_Reminders", "drawer_click")
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    onRemainderClick()
                }

                3 -> {
                    logAnalyticsEvent("Drawer_Diary_Lock", "drawer_click")
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId = R.id.action_mainFragment_to_changePasswordFragment
                    )
                }
                4 -> {
                    logAnalyticsEvent("Drawer_Language", "drawer_click")
                    if (view != null) {
                        binding?.parentLayout?.showSnackbar(message = getString(R.string.coming_soon))
                        binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    }
                    return@DrawerItemAdapter
                }

                5 -> {
                    activity?.privacyPolicyUrl()
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    return@DrawerItemAdapter
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        imagePicker = ImagePickerDelegate(this, onPickerClosed = {
            sessionManagerRepo.bypassSecurityLogin(false)
            if (view != null) {
                binding?.bottomNavCreateNote?.clearChecked()
            }
        }, onImagePicked = { uri: Uri?, file: File? ->
            sessionManagerRepo.bypassSecurityLogin(false)
            showImageCropDialog(imagePath = file?.path ?: return@ImagePickerDelegate, btnDone = {
                createNotesViewModel.sendAction(
                    action = CreateNotesState.ImagePicked(
                        imageUri = it
                    )
                )
            }, closeDialog = {
                if (view != null) {
                    binding?.bottomNavCreateNote?.clearChecked()
                }
            })
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeHost = childFragmentManager.findFragmentById(R.id.nav_host_home) as NavHostFragment
        libraryHost =
            childFragmentManager.findFragmentById(R.id.nav_host_library) as NavHostFragment
        calendarHost =
            childFragmentManager.findFragmentById(R.id.nav_host_calendar) as NavHostFragment

        childFragmentManager.beginTransaction().hide(libraryHost).hide(calendarHost).show(homeHost)
            .commitNow()

        activeNavHost = homeHost
        // Initial state for backstack
        backStack.push(R.id.btnHome)

        binding?.bottomNav?.clearChecked()
        setupNavControllerListener()
        setupBottomNav()
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
        setupDrawer()
        observeMainState()
        loadRewardedAdd()
        binding?.apply {
            ViewCompat.setOnApplyWindowInsetsListener(createNoteBottomBar) { v, insets ->
                val keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val systemBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom

                // If keyboard is shown, keyboardHeight will be > 0
                // We move the bar UP by the keyboard height minus system bar height
                val moveUpBy = if (keyboardHeight > 0) {
                    keyboardHeight - systemBarHeight
                } else {
                    0
                }

                v.translationY = -moveUpBy.toFloat()
                insets
            }
        }
    }

    private fun setupBottomNavBar() {
        binding?.apply {
            bottomNavCreateNote.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnBackground -> {
                            logAnalyticsEvent("Add_Note_Background", "toolbar_click")
                            showBackgroundDialog(
                                sessionManagerRepo = sessionManagerRepo,
                                adapterMultiImageAdapter = multiImageAdapter, closeDialog = {
                                    if (view != null) {
                                        binding?.bottomNavCreateNote?.clearChecked()
                                    }
                                })
                        }
                        R.id.btn_hash_tag -> {
                            logAnalyticsEvent("Add_Note_Hash_Tag", "toolbar_click")
                            createNotesViewModel.sendAction(CreateNotesState.TagAction)
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(50)
                                if (view != null) group.clearChecked()
                            }
                        }
                        R.id.btn_media -> {
                            logAnalyticsEvent("Add_Note_Media", "toolbar_click")
                            showReward()
                        }
                        R.id.btn_text -> {
                            logAnalyticsEvent("Add_Note_Text", "toolbar_click")
                            showEditTexDialog(
                                sessionManagerRepo = sessionManagerRepo,
                                closeDialog = {
                                    if (view != null) {
                                        binding?.bottomNavCreateNote?.clearChecked()
                                    }
                                },
                                fontSelectionListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.FontAction(it))
                                },
                                textAlignmentListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.TextAlignment(it))
                                },
                                textBoldListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.HeadingSize(it))
                                },
                                textColorListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.TextColor(it))
                                },
                                colorPalette = (activity as? MainActivity)?.getColorPalette() ?: listOf()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupBottomNav() {
        binding?.bottomNav?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val targetHost = when (checkedId) {
                R.id.btnHome -> {
                    homeHost
                }
                R.id.btn_library -> {
                    logAnalyticsEvent("Home_Screen_Library", "tab_switch")
                    libraryHost
                }
                R.id.btn_calendar ->{
                    logAnalyticsEvent("Home_Screen_Calendar_Opened", "tab_switch")
                    calendarHost
                }
                else -> return@addOnButtonCheckedListener
            }

            if (targetHost == activeNavHost) {
                val navController = targetHost.navController
                val startDestinationId = navController.graph.startDestinationId
                navController.popBackStack(startDestinationId, false)
                return@addOnButtonCheckedListener
            }

            // Update manual backstack
            backStack.remove(checkedId) // Prevent duplicates
            backStack.push(checkedId)

            childFragmentManager.beginTransaction()
                .hide(activeNavHost ?: return@addOnButtonCheckedListener).show(targetHost)
                .commitNowAllowingStateLoss()

            activeNavHost = targetHost
            setupNavControllerListener()

            // Handle visibility based on tab
            binding?.apply {
                when (checkedId) {
                    R.id.btnHome -> {
                        icAddNotes.visibility = View.VISIBLE
                        ivRemainder.visibility = View.VISIBLE
                    }
                    else -> {
                        icAddNotes.visibility = View.GONE
                        ivRemainder.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupDrawer() {
        binding?.apply {
            val themeColor=getCurrentThemeColor(sessionManagerRepo)
            binding?.parentLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            drawerLayout.drawerItems.run {
                adapter = drawerItemAdapter
                hasFixedSize()
            }
            drawerItemAdapter.submitList((activity as MainActivity).getDrawerItemList())
            drawerLayout.apply {
                ivBack.setOnClickListener {
                    parentLayout.closeDrawer(GravityCompat.START)
                }
                ivEditProfile.backgroundTintList=ColorStateList.valueOf(themeColor)
                ivEditProfile.setOnClickListener {
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId = R.id.action_mainFragment_to_editProfileFragment
                    )
                }


                profileLayout.setOnClickListener {
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId = R.id.action_mainFragment_to_editProfileFragment
                    )
                }
                val profilePic = sessionManagerRepo.getprofilePic().orEmpty()
                if (profilePic.isNotEmpty()) {
                    ivPlacHolder.visibility = View.INVISIBLE
                    ivProfile.visibility = View.VISIBLE
                    ivProfile.setImage(drawable = profilePic.toUri())
                } else {

                    ivPlacHolder.visibility = View.VISIBLE
                    ivProfile.visibility = View.INVISIBLE
                }
                val savedName = viewModel.getName()
                if (savedName?.isNotEmpty() == true) {
                    tvName.text = savedName
                }
            }
        }
    }

    private fun setupNavControllerListener() {
        val navHost = activeNavHost ?: return
        val navController = navHost.navController
        navHostListeners[navHost]?.let { oldListener ->
            navController.removeOnDestinationChangedListener(oldListener)
        }
        val newListener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (view != null) {
                binding?.headerTitle?.text = destination.label
                handleDestinationChange(destination.id)
            }
        }
        navController.addOnDestinationChangedListener(newListener)
        navHostListeners[navHost] = newListener
    }

    private fun handleDestinationChange(destinationId: Int) {
        binding?.apply {
            when (destinationId) {
                R.id.createNotesFragment -> {
                    createNoteBottomBar.visibility = View.VISIBLE
                    bottomNav.visibility = View.GONE
                    icAddNotes.visibility = View.INVISIBLE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    ivCreateNote.visibility = View.VISIBLE
                    ivKabab.visibility = View.GONE
                    setNoteHeader()
                }

                R.id.homeFragment -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility = View.VISIBLE
                    bottomNav.visibility = View.VISIBLE
                    ivMenu.visibility = View.VISIBLE
                    ivBack.visibility = View.INVISIBLE
                    ivKabab.visibility = View.GONE
                    setHomeTabChecked()
                    setDefaultNavHeader()
                }

                R.id.addTagsFragment -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.GONE
                    bottomNav.visibility = View.GONE
                    icAddNotes.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    ivKabab.visibility = View.GONE
                    ivRemainder.visibility = View.GONE
                    setTagsHeader()
                }

                R.id.previewFragment, R.id.previewFragment2 -> {
                    bottomNav.visibility = View.GONE
                    ivKabab.visibility = View.VISIBLE
                    headerSave.visibility = View.GONE
                    ivRemainder.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    icAddNotes.visibility = View.GONE
                }

                R.id.remainderFragment -> {
                    bottomNav.visibility = View.GONE
                    ivKabab.visibility = View.VISIBLE
                    headerSave.visibility = View.GONE
                    ivRemainder.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    icAddNotes.visibility = View.GONE
                    ivKabab.visibility = View.GONE
                }

                else -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility = View.INVISIBLE
                    bottomNav.visibility = View.VISIBLE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    ivKabab.visibility = View.GONE
                    ivRemainder.visibility = View.GONE
                }
            }
        }
    }

    // Helper to ensure UI reflects Home state correctly
    private fun setHomeTabChecked() {
        if (binding?.bottomNav?.checkedButtonId != R.id.btnHome) {
            binding?.bottomNav?.check(R.id.btnHome)
        }
    }

    private fun setClickListeners() {
        binding?.apply {
            ivMenu.setOnClickListener {
                logAnalyticsEvent("Drawer_Button", "icon_click")
                parentLayout.openDrawer(GravityCompat.START)
            }
            ivBack.setOnClickListener {
                onBackTriggered()
            }
            headerSave.setOnClickListener {
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
            ivRemainder.setOnClickListener {
                logAnalyticsEvent("Home_Screen_Reminder", "icon_click")
                onRemainderClick()
            }
            icAddNotes.setOnClickListener {
                logAnalyticsEvent("Home_Screen_Add_Note", "button_click")
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                activeNavHost?.findNavController()?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId = R.id.action_homeFragment_to_createNotesFragment2,
                    Bundle().apply {
                        putBoolean(FROM_SCREEN, true)
                    })
            }
        }
    }
    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        val params = Bundle().apply {
            putString("action_label", label)
        }
        mFirebaseAnalytics.logEvent(eventName, params)
    }
    private fun onBackTriggered() {
        val currentHost = activeNavHost ?: return
        val navController = currentHost.findNavController()
        val currentDestId = navController.currentDestination?.id

        when (currentDestId) {
            R.id.addTagsFragment -> {
//                createNotesViewModel.sendAction(
//                    action = CreateNotesState.AddTag(
//                        tag = "Personal",
//                        createNoteEntity = createNotesViewModel.noteState.value
//                    )
//                )
                navController.navigateUp()
            }
            else -> {
                // First check internal backstack of the active nav host (e.g. Preview -> Home)
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    // Handle Bottom Navigation Backstack (Calendar -> Library -> Home)
                    handleBottomNavBackstack()
                }
            }
        }
    }

    private fun handleBottomNavBackstack() {
        // If the drawer is open, just close it and stop
        if (view != null && binding?.parentLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.parentLayout?.closeDrawer(GravityCompat.START)
            return
        }

        if (backStack.size > 1) {
            // Remove the current tab from history
            backStack.pop()

            // Get the previous tab ID
            val previousTabId = backStack.peek()

            if (view != null && previousTabId != null) {
                // This will trigger the listener in setupBottomNav
                // and handle fragment switching automatically
                binding?.bottomNav?.check(previousTabId)
            }
        } else {
            // We are on the last remaining tab in the history (usually Home)
            // Check if the current visible tab is NOT Home.
            // If it's not Home, move to Home first. If it IS Home, show exit dialog.
            if (binding?.bottomNav?.checkedButtonId != R.id.btnHome) {
                binding?.bottomNav?.check(R.id.btnHome)
            } else {
                showFeedBackDialog {
                    activity?.finish()
                }
            }
        }
    }

    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()
        binding?.parentLayout?.loadBackground(
            resourceId = currentTheme,
            placeholder = R.drawable.theme_1
        )
        // Apply colors to FAB and Bottom Nav
        applyDynamicTheme(currentTheme)
    }
    private fun setNoteHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.add_note)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.VISIBLE
            // RESTORE the Note Save listener
            headerSave.setOnClickListener {
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
        }
    }
    private fun setProfileHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.edit_profile)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.VISIBLE
        }
    }
    private fun setTagsHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.tags)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.VISIBLE

            headerSave.setOnClickListener {
                // Find the child fragment inside the currently active NavHost
                val currentFragment = activeNavHost?.childFragmentManager?.fragments?.find { it is AddTagsFragment }

                // Cast and call the public save function
                (currentFragment as? AddTagsFragment)?.handleSaveAction()
            }
        }
    }

    private fun setDefaultNavHeader() {
        binding?.apply {
            ivMenu.setImageResource(R.drawable.ic_menu)
            ivRemainder.visibility = View.VISIBLE
            headerSave.visibility = View.GONE
            ivRemainder.setImageResource(R.drawable.notification)
        }
    }

    fun observeMainState() {
        viewLifecycleOwner.lifecycleScope.launch {
            createNotesViewModel.noteState.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                if (view != null) {
                    binding?.ivCreateNote?.loadImage(
                        it?.backgroundRes, placeholder = 0
                    )
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackTriggered()
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    private fun onRemainderClick() {
        (activity as? MainActivity)?.requestExactAlarmPermission {
            activeNavHost?.findNavController()?.safeNav(
                currentDestId = R.id.homeFragment,
                actionId = R.id.action_homeFragment_to_remainderFragment
            )
        }
    }
    // 1. Add this launcher at the top of MainFragment class
    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10) // Limit to 10 images
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                // Send each image to the ViewModel state
                createNotesViewModel.sendAction(
                    action = CreateNotesState.ImagePicked(imageUri = uri)
                )
            }
            AppLogger.createLog("MultiPicker", "Sent ${uris.size} images to ViewModel")
        }
        // Clear bottom nav selection after picking
        if (view != null) {
            binding?.bottomNavCreateNote?.clearChecked()
        }
    }

    // 2. Update the pickImage() function to use the new launcher
    private fun pickImage() {
        pickPhotDialog(
            sessionManagerRepo = sessionManagerRepo,
            cameraCallBack = {
                logAnalyticsEvent("Add_Note_Take_a_Photo", "media_source")
            sessionManagerRepo.bypassSecurityLogin(true)
            imagePicker.pickFromCameraWithPermission() // Keep camera for single photos
        }, galleryCallBack = {
                logAnalyticsEvent("Add_Note_Upload_From_Gallery", "media_source")
            // Trigger the Multi-Photo Picker instead of the old single picker
            pickMultipleMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }, onDismiss = {
            if (view != null) {
                binding?.bottomNavCreateNote?.clearChecked()
            }
        })
    }
    override fun onDestroyView() {
        super.onDestroyView()
        navHostListeners.forEach { (host, listener) ->
            host.navController.removeOnDestinationChangedListener(listener)
        }
        navHostListeners.clear()
    }

    private fun loadRewardedAdd() {
        rewardedAdsConfig.loadRewardedAd(adType = RewardedAdKey.IMAGE_MORE_THAN_ONE)
    }

    private fun showReward() {
        rewardedAdsConfig.showRewardedAd(
            activity,
            adType = RewardedAdKey.IMAGE_MORE_THAN_ONE,
            listener = object : RewardedOnShowCallBack {
                override fun onAdFailedToShow() = pickImage()
                override fun onUserEarnedReward() {
                    pickImage()
                }
            })
    }


    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = when (themeResId) {
            R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
            R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
            R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
            R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
            R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
            else -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
        }

        binding?.apply {
            // 1. Update FAB separately (FAB is NOT a toggle button)
            // We use a simple ColorStateList that applies to all states
            icAddNotes.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)

            // 2. Update Bottom Navigation Buttons (These ARE toggle buttons)
            val navButtons = listOf(btnHome, btnLibrary, btnCalendar)

            val navStates = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val navColors = intArrayOf(
                themeColor,
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            val navColors2 = intArrayOf(
                themeColor,
                ContextCompat.getColor(requireContext(), android.R.color.black)
            )
            val navSelector = android.content.res.ColorStateList(navStates, navColors)
            val navSelector2 = android.content.res.ColorStateList(navStates, navColors2)

            navButtons.forEach { button ->
                button.backgroundTintList = navSelector

                // --- NEW: Dynamic Shadow/Glow Handling ---
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    // We set the shadow color to the theme color
                    // This will only be visible when the button has elevation (is checked)
                    button.outlineSpotShadowColor = themeColor
                    button.outlineAmbientShadowColor = themeColor
                }
            }

            val createNoteButtons = listOf(btnBackground, btnHashTag, btnMedia, btnText)

            createNoteButtons.forEach { button ->
                button.iconTint = navSelector2
                button.setTextColor(navSelector2)

            }

        }

    }

}