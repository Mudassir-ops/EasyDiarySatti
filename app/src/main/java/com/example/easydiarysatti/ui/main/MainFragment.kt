package com.example.easydiarysatti.ui.main

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.easydiarysatti.ads.rewarded.RewardedInterAdsConfig
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnLoadCallBack
import com.example.easydiarysatti.ads.rewarded.callbacks.RewardedOnShowCallBack
import com.example.easydiarysatti.ads.rewarded.enums.RewardedAdKey
import com.example.easydiarysatti.ads.rewarded.enums.RewardedInterAdKey
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.privacyPolicyUrl
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showSnackbar
import com.example.easydiarysatti.ui.createnote.CreateNotesFragment
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.home.HomeFragment
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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.easydiarysatti.AppLogger
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.interstitial.InterstitialAdsConfig
import com.example.easydiarysatti.ads.interstitial.callbacks.InterstitialOnShowCallBack
import com.example.easydiarysatti.ads.interstitial.enums.InterAdKey
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.ads.rewarded.RewardedLoadingDialog
import com.example.easydiarysatti.paywalls.BillingManager
import com.example.easydiarysatti.paywalls.ProAccessManager
import com.example.easydiarysatti.paywalls.RemoveAdsDialog
import com.example.easydiarysatti.paywalls.RewardedGateDialog
import com.example.easydiarysatti.ui.edittags.AddTagsFragment
import com.example.easydiarysatti.utills.CreateNoteOptionsBottomSheet
import com.example.easydiarysatti.utills.ExitPopupDialog
import com.example.easydiarysatti.utills.getCurrentThemeColor
import com.google.firebase.analytics.FirebaseAnalytics
import kotlin.getValue

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var calendarHost: NavHostFragment
    private lateinit var libraryHost: NavHostFragment
    private lateinit var homeHost: NavHostFragment
    private lateinit var navController: NavController
    private val viewModelNative: ViewModelNative by activityViewModels()
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private val viewModelCreateNote: CreateNotesViewModel by activityViewModels()
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val viewModel by activityViewModels<NameViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate
    private var activeNavHost: NavHostFragment? = null
    private val bannerViewModel by activityViewModels<ViewModelBanner>()
    private val backStack = LinkedList<Int>()

    @Inject
    lateinit var interstitialAdsConfig: InterstitialAdsConfig
    private val navHostListeners =
        mutableMapOf<NavHostFragment, NavController.OnDestinationChangedListener>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    @Inject
    lateinit var rewardedAdsConfig: RewardedAdsConfig

    @Inject
    lateinit var rewardedInterAdsConfig: RewardedInterAdsConfig

    private var pendingReminderAction: (() -> Unit)? = null
    private var pendingRewardGate: String? = null
    private var innerDestBeforePaywall: Int? = null
    private var paywallOpenedFromCreateNote: Boolean = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingReminderAction?.invoke()
            }
            pendingReminderAction = null
        }

    private val bgGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            if (uri == null) return@registerForActivityResult
            multiImageAdapter.setGalleryImage(uri)
            applyGalleryBackground(uri)
            multiImageAdapter.onUploadClickIntercept?.invoke()
            multiImageAdapter.onUploadClickIntercept = null
        }

    @Inject
    lateinit var sharedPref: com.example.easydiarysatti.ads.manager.SharedPreferenceUtils

    private val proAccessManager by lazy {
        ProAccessManager(
            activity       = requireActivity() as androidx.fragment.app.FragmentActivity,
            sharedPref     = sharedPref,
            billingManager = billingManager
        )
    }

    private val billingManager: BillingManager by lazy {
        BillingManager(
            activity          = requireActivity(),
            onPurchaseSuccess = { productId ->
                if (productId == ProAccessManager.REMOVE_ADS_PRODUCT_ID) {
                    sharedPref.isAppPurchased = true
                    refreshPremiumBadges()
                }
            }
        )
    }

    private val multiImageAdapter: MultiImageAdapter by lazy {
        MultiImageAdapter(
            items = (activity as MainActivity).getBgThemes(),
            onUploadClick = {
                bgGalleryLauncher.launch("image/*")
            },
            onImageClick = { source, isPremium ->
                if (isPremium && !sharedPref.isAppPurchased) {
                    RewardedGateDialog.show(
                        fragmentManager = childFragmentManager,
                        onWatchAd = {
                            pendingRewardGate = "bg"
                            pendingBgSource = source
                            loadAndShowReward(RewardedAdKey.VIDEO_PRO_BG)
                        },
                        onSubscribe = {
                            captureInnerDestBeforePaywall()
                            proAccessManager.onPremiumIconClicked()
                        },
                        title = "Premium Background",
                        subtitle = "This background is for premium users. Watch a short video to use it, or subscribe for unlimited access.",
                        unlockLinkText = "unlock this background"
                    )
                } else {
                    when (source) {
                        is com.example.easydiarysatti.utills.BgItem.DrawableRes  -> applyBackground(source.resId)
                        is com.example.easydiarysatti.utills.BgItem.GalleryImage -> applyGalleryBackground(source.uri)
                        null -> Unit
                    }
                }
            }
        )
    }

    private var pendingBgSource: com.example.easydiarysatti.utills.BgItem? = null

    private fun applyBackground(resourceId: Int?) {
        if (view == null) return
        val iv = binding?.ivCreateNote ?: return
        if (resourceId != null && resourceId != 0) {
            com.bumptech.glide.Glide.with(iv.context)
                .load(resourceId)
                .override(
                    iv.width.takeIf { it > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                    iv.height.takeIf { it > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
                )
                .centerCrop()
                .into(iv)
        } else {
            com.bumptech.glide.Glide.with(iv.context).clear(iv)
            iv.setImageDrawable(null)
        }
        createNotesViewModel.noteState.value?.let { current ->
            createNotesViewModel.setupNoteEntity(
                current.copy(backgroundRes = resourceId, bgImageUri = null)
            )
        }
        createNotesViewModel.sendAction(CreateNotesState.ChangeBgUri(bgImageUri = ""))
        createNotesViewModel.sendAction(
            CreateNotesState.ChangeBg(bgImageRes = resourceId ?: return)
        )
    }

    private fun applyGalleryBackground(uri: android.net.Uri) {
        if (view == null) return
        val iv = binding?.ivCreateNote ?: return
        com.bumptech.glide.Glide.with(iv.context)
            .load(uri)
            .override(
                iv.width.takeIf  { it > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                iv.height.takeIf { it > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
            )
            .centerCrop()
            .into(iv)
        createNotesViewModel.noteState.value?.let { current ->
            createNotesViewModel.setupNoteEntity(
                current.copy(bgImageUri = uri.toString(), backgroundRes = null)
            )
        }
        createNotesViewModel.sendAction(CreateNotesState.ChangeBgUri(bgImageUri = uri.toString()))
    }

    // ── Pro Card Slider ──────────────────────────────────────────────────────
    private var proCardAutoScrollHandler: Handler? = null
    private var proCardAutoScrollRunnable: Runnable? = null
    private val proCardDotViews = mutableListOf<ImageView>()

    // ─────────────────────────────────────────────────────────────────────────
    //  CHANGE 1: drawerItemAdapter — case 0 (Edit Tags)
    //    • REMOVED: binding?.headerSave?.visibility = View.VISIBLE  (was wrong)
    //    • ADDED:   closeDrawer() before navigating                 (was missing)
    // ─────────────────────────────────────────────────────────────────────────
    private val drawerItemAdapter: DrawerItemAdapter by lazy {
        DrawerItemAdapter(onNoteItemClick = {
            when (it) {

                // 0 — Edit Tags
                0 -> {
                    logAnalyticsEvent("Drawer_Edit_Tags", "drawer_click")
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START) // ← ADDED
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId      = R.id.action_mainFragment_to_addTagsFragment2,
                        bundle        = Bundle().apply { putBoolean(FROM_SCREEN, true) }
                    )
                    // ← REMOVED: binding?.headerSave?.visibility = View.VISIBLE
                }

                // 1 — Favorites
                1 -> {
                    logAnalyticsEvent("Drawer_Favorites", "drawer_click")
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId      = R.id.action_mainFragment_to_favoritesFragment
                    )
                }

                // 2 — Draft
                2 -> {
                    logAnalyticsEvent("Drawer_Draft", "drawer_click")
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId      = R.id.action_mainFragment_to_draftNotesFragment
                    )
                }

                // 3 — Color Theme
                3 -> {
                    logAnalyticsEvent("Drawer_Color_Theme", "drawer_click")
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId      = R.id.action_mainFragment_to_themesFragment
                    )
                }

                // 4 — Reminders
                4 -> {
                    logAnalyticsEvent("Drawer_Reminders", "drawer_click")
                    if (view != null) binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    onRemainderClick()
                }

                // 5 — Diary Lock
                5 -> {
                    logAnalyticsEvent("Drawer_Diary_Lock", "drawer_click")
                    findNavController().safeNav(
                        currentDestId = R.id.mainFragment,
                        actionId      = R.id.action_mainFragment_to_changePasswordFragment
                    )
                }

                // 6 — Language
                6 -> {
                    logAnalyticsEvent("Drawer_Language", "drawer_click")
                    if (view != null) {
                        binding?.parentLayout?.showSnackbar(message = getString(R.string.coming_soon))
                        binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    }
                    return@DrawerItemAdapter
                }

                // 7 — Privacy Policy
                7 -> {
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
                    action = CreateNotesState.ImagePicked(imageUri = it)
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
        billingManager.startConnection()
        homeHost     = childFragmentManager.findFragmentById(R.id.nav_host_home)     as NavHostFragment
        libraryHost  = childFragmentManager.findFragmentById(R.id.nav_host_library)  as NavHostFragment
        calendarHost = childFragmentManager.findFragmentById(R.id.nav_host_calendar) as NavHostFragment

        childFragmentManager.beginTransaction()
            .hide(libraryHost).hide(calendarHost).show(homeHost)
            .commitNow()

        activeNavHost = homeHost
        backStack.push(R.id.btnHome)

        binding?.bottomNav?.clearChecked()
        setupNavControllerListener()
        setupOuterNavPaywallListener()
        setupBottomNav()
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
        setupDrawer()
        observeMainState()
        refreshPremiumBadges()
        viewModelNative.loadNativeAd(NativeAdKey.EXIT)

        if (paywallOpenedFromCreateNote) {
            paywallOpenedFromCreateNote = false
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    val innerNav = homeHost.findNavController()
                    if (innerNav.currentDestination?.id != R.id.createNotesFragment) {
                        innerNav.navigate(R.id.action_homeFragment_to_createNotesFragment2)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PaywallNav", "onViewCreated restore failed: ${e.message}")
                }
            }
        }

        binding?.apply {
            ViewCompat.setOnApplyWindowInsetsListener(createNoteBottomBar) { v, insets ->
                val keyboardHeight  = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                val systemBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                val moveUpBy = if (keyboardHeight > 0) keyboardHeight - systemBarHeight else 0
                v.translationY = -moveUpBy.toFloat()
                insets
            }
        }
    }

    // ── Home Fragment selection wiring ───────────────────────────────────────

    private fun wireHomeFragment() {
        val homeFragment = getHomeFragment() ?: return
        homeFragment.onSelectionChanged = { isActive, count ->
            binding?.apply {
                if (isActive) {
                    ivDeleteAll.visibility = View.VISIBLE
                    ivRemainder.visibility = View.VISIBLE
                    ivPremium.visibility   = View.GONE
                } else {
                    ivDeleteAll.visibility = View.GONE
                    ivRemainder.visibility = View.VISIBLE
                    ivPremium.visibility   =
                        if (sharedPref.isAppPurchased) View.GONE else View.VISIBLE
                    headerTitle.text = getString(R.string.title_home)
                }
            }
        }
    }

    private fun getHomeFragment(): HomeFragment? {
        return homeHost.childFragmentManager.fragments
            .firstOrNull { it is HomeFragment } as? HomeFragment
    }

    private fun showExitPopupFromConfig() {
        if (!sharedPref.shouldShowExitPopup()) {
            activity?.finish()
            return
        }
        val variant = when (sharedPref.getExitPopupVariant()) {
            "Variant B" -> ExitPopupDialog.Variant.WITH_RATING
            "Variant C" -> ExitPopupDialog.Variant.WITH_NATIVE
            else        -> ExitPopupDialog.Variant.SIMPLE
        }
        val nativeAd = if (variant == ExitPopupDialog.Variant.WITH_NATIVE) {
            viewModelNative.adMapLiveData.value?.get(NativeAdKey.EXIT)
        } else null

        ExitPopupDialog.show(
            context  = requireContext(),
            variant  = variant,
            nativeAd = nativeAd,
            onExit   = { activity?.finish() }
        )
    }

    private fun setupBottomNavBar() {
        binding?.apply {
            bottomNavCreateNote.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnBackground -> {
                            logAnalyticsEvent("Add_Note_Background", "toolbar_click")
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(50)
                                if (view != null) group.clearChecked()
                            }
                            showBackgroundDialog(
                                sessionManagerRepo       = sessionManagerRepo,
                                adapterMultiImageAdapter = multiImageAdapter,
                                closeDialog = {
                                    if (view != null) binding?.bottomNavCreateNote?.clearChecked()
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
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(50)
                                if (view != null) group.clearChecked()
                            }
                            captureInnerDestBeforePaywall()
                            proAccessManager.checkMediaNoteAccess(
                                fragmentManager = requireActivity().supportFragmentManager,
                                fromCreateNote  = true,
                                onWatchAd = {
                                    innerDestBeforePaywall = null
                                    getCreateNotesFragment()?.paywallCurrentlyOpen = false
                                    pendingRewardGate = "media"
                                    loadAndShowReward(RewardedAdKey.VIDEO_PRO_MEDIA)
                                },
                                onAllowed = {
                                    innerDestBeforePaywall = null
                                    getCreateNotesFragment()?.paywallCurrentlyOpen = false
                                    pickImage()
                                },
                                onSubscribe = {
                                    findNavController().safeNav(
                                        currentDestId = R.id.mainFragment,
                                        actionId      = R.id.action_global_mainPaywallFragment,
                                        bundle        = com.example.easydiarysatti.paywalls.MainPaywallFragment.args(
                                            fromCreateNote   = true,
                                            fromRewardedGate = true
                                        )
                                    )
                                }
                            )
                        }
                        R.id.btn_text -> {
                            logAnalyticsEvent("Add_Note_Text", "toolbar_click")
                            showEditTexDialog(
                                sessionManagerRepo = sessionManagerRepo,
                                closeDialog = {
                                    if (view != null) binding?.bottomNavCreateNote?.clearChecked()
                                },
                                fontSelectionListener      = { createNotesViewModel.sendAction(CreateNotesState.FontAction(it)) },
                                textAlignmentListener      = { createNotesViewModel.sendAction(CreateNotesState.TextAlignment(it)) },
                                textBoldListener           = { createNotesViewModel.sendAction(CreateNotesState.HeadingSize(it)) },
                                textColorListener          = { createNotesViewModel.sendAction(CreateNotesState.TextColor(it)) },
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
                R.id.btnHome     -> homeHost
                R.id.btn_library -> {
                    viewModelNative.loadNativeAd(NativeAdKey.LIBRARY)
                    logAnalyticsEvent("Home_Screen_Library", "tab_switch")
                    libraryHost
                }
                R.id.btn_calendar -> {
                    preLoadNextAd(BannerAdKey.CALENDAR)
                    logAnalyticsEvent("Home_Screen_Calendar_Opened", "tab_switch")
                    calendarHost
                }
                else -> return@addOnButtonCheckedListener
            }

            if (targetHost == activeNavHost) {
                val nc = targetHost.navController
                nc.popBackStack(nc.graph.startDestinationId, false)
                return@addOnButtonCheckedListener
            }

            backStack.remove(checkedId)
            backStack.push(checkedId)

            childFragmentManager.beginTransaction()
                .hide(activeNavHost ?: return@addOnButtonCheckedListener).show(targetHost)
                .commitNowAllowingStateLoss()

            activeNavHost = targetHost
            setupNavControllerListener()

            binding?.apply {
                when (checkedId) {
                    R.id.btnHome -> {
                        icAddNotes.visibility  = View.VISIBLE
                        ivRemainder.visibility = View.VISIBLE
                    }
                    else -> {
                        icAddNotes.visibility  = View.GONE
                        ivRemainder.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupDrawer() {
        binding?.apply {
            val themeColor = getCurrentThemeColor(sessionManagerRepo)
            binding?.parentLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            drawerLayout.drawerItems.run {
                adapter = drawerItemAdapter
                hasFixedSize()
            }
            drawerItemAdapter.submitList((activity as MainActivity).getDrawerItemList())
            setupProCardSlider()
            drawerLayout.apply {
                ivBack.setOnClickListener {
                    parentLayout.closeDrawer(GravityCompat.START)
                }
                ivEditProfile.backgroundTintList = ColorStateList.valueOf(themeColor)
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
                    ivProfile.visibility    = View.VISIBLE
                    ivProfile.setImage(drawable = profilePic.toUri())
                } else {
                    ivPlacHolder.visibility = View.VISIBLE
                    ivProfile.visibility    = View.INVISIBLE
                }
                val savedName = viewModel.getName()
                if (savedName?.isNotEmpty() == true) {
                    tvName.text = savedName
                }
            }
        }
    }

    private fun setupProCardSlider() {
        val binding = binding ?: return
        val viewPager  = binding.drawerLayout.proCardViewPager
        val dotsLayout = binding.drawerLayout.proCardDots
        (viewPager.getChildAt(0) as? RecyclerView)?.let { rv ->
            rv.overScrollMode = View.OVER_SCROLL_NEVER
        }
        val adapter = ProCardAdapter(
            onRemoveAdsClick = {
                val drawer = binding.parentLayout
                drawer.closeDrawer(androidx.core.view.GravityCompat.START)
                drawer.postDelayed({
                    if (!isAdded || view == null) return@postDelayed
                    proAccessManager.onRemoveAdsClicked(requireActivity().supportFragmentManager)
                }, 600)
            },
            onSubscribeClick = {
                val drawer = binding.parentLayout
                drawer.closeDrawer(androidx.core.view.GravityCompat.START)
                drawer.postDelayed({
                    if (!isAdded || view == null) return@postDelayed
                    captureInnerDestBeforePaywall()
                    proAccessManager.onPremiumIconClicked()
                }, 600)
            }
        )
        viewPager.adapter = adapter

        proCardDotViews.clear()
        dotsLayout.removeAllViews()
        repeat(adapter.itemCount) { i ->
            val dot = ImageView(requireContext()).apply {
                val size   = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp)
                val params = LinearLayout.LayoutParams(size, size).also { it.setMargins(6, 0, 6, 0) }
                layoutParams = params
                setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
            }
            dotsLayout.addView(dot)
            proCardDotViews.add(dot)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                proCardDotViews.forEachIndexed { index, dot ->
                    dot.setImageResource(
                        if (index == position) R.drawable.dot_active else R.drawable.dot_inactive
                    )
                }
            }
        })

        proCardAutoScrollHandler = Handler(Looper.getMainLooper())
        proCardAutoScrollRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || view == null) return
                val next = (viewPager.currentItem + 1) % adapter.itemCount
                viewPager.setCurrentItem(next, true)
                proCardAutoScrollHandler?.postDelayed(this, 2000L)
            }
        }
        proCardAutoScrollHandler?.postDelayed(proCardAutoScrollRunnable!!, 2000L)
    }

    private fun stopProCardAutoScroll() {
        proCardAutoScrollRunnable?.let { proCardAutoScrollHandler?.removeCallbacks(it) }
        proCardAutoScrollHandler  = null
        proCardAutoScrollRunnable = null
    }

    private fun setupOuterNavPaywallListener() {
        var previousDestinationId: Int? = null
        findNavController().addOnDestinationChangedListener { _, destination, _ ->
            android.util.Log.d("OuterNav", "dest=${destination.id}  prev=$previousDestinationId  view=${view != null}")

            // ── draftNotesFragment ────────────────────────────────────────────
            if (view != null && destination.id == R.id.draftNotesFragment) {
                handleDestinationChange(R.id.draftNotesFragment)
            }
            if (view != null
                && destination.id == R.id.mainFragment
                && previousDestinationId == R.id.draftNotesFragment) {
                val innerDest = try { activeNavHost?.navController?.currentDestination?.id } catch (e: Exception) { null }
                handleDestinationChange(innerDest ?: R.id.homeFragment)
                if (innerDest == null || innerDest == R.id.homeFragment) setDefaultNavHeader()
            }

            // ── addTagsFragment2 (Drawer → Edit Tags) ────────────────────────
            //
            //  AddTagsFragment draws its OWN themed toolbar (drawerToolbar) in
            //  manage mode. All MainFragment needs to do here is HIDE its own
            //  toolbar views so they don't bleed through.
            if (view != null && destination.id == R.id.addTagsFragment2) {
                binding?.apply {
                    ivMenu.visibility              = View.GONE
                    ivBack.visibility              = View.GONE
                    headerTitle.text               = ""
                    ivRemainder.visibility         = View.GONE
                    headerSave.visibility          = View.GONE
                    ivPremium.visibility           = View.GONE
                    ivSavePremiumBadge.visibility  = View.GONE
                    ivDeleteAll.visibility         = View.GONE
                    icAddNotes.visibility          = View.GONE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    bottomNav.visibility           = View.GONE
                }
            }

            // ── Returning from addTagsFragment2 back to mainFragment ──────────
            if (view != null
                && destination.id == R.id.mainFragment
                && previousDestinationId == R.id.addTagsFragment2) {
                val innerDest = try { activeNavHost?.navController?.currentDestination?.id } catch (e: Exception) { null }
                handleDestinationChange(innerDest ?: R.id.homeFragment)
                if (innerDest == null || innerDest == R.id.homeFragment) setDefaultNavHeader()
            }

            // ── Paywall ───────────────────────────────────────────────────────
            if (destination.id == R.id.mainFragment && previousDestinationId == R.id.mainPaywallFragment) {
                innerDestBeforePaywall = null
                val createNoteInStack = try {
                    homeHost.childFragmentManager.fragments
                        .any { it is com.example.easydiarysatti.ui.createnote.CreateNotesFragment }
                } catch (e: Exception) { false }
                if (!createNoteInStack) paywallOpenedFromCreateNote = false
            }

            previousDestinationId = destination.id
        }
    }

    private fun captureInnerDestBeforePaywall() {
        innerDestBeforePaywall = try {
            homeHost.findNavController().currentDestination?.id
        } catch (e: Exception) { null }
        android.util.Log.d("PaywallNav", "captureInnerDestBeforePaywall → $innerDestBeforePaywall")

        if (innerDestBeforePaywall == R.id.createNotesFragment) {
            getCreateNotesFragment()?.let { createNote ->
                createNote.snapshotDraftToViewModel()
                createNote.paywallCurrentlyOpen = true
            }
            paywallOpenedFromCreateNote = true
        }
    }

    private fun getCreateNotesFragment(): com.example.easydiarysatti.ui.createnote.CreateNotesFragment? {
        return homeHost.childFragmentManager.fragments
            .firstOrNull { it is com.example.easydiarysatti.ui.createnote.CreateNotesFragment }
                as? com.example.easydiarysatti.ui.createnote.CreateNotesFragment
    }

    private fun setupNavControllerListener() {
        val navHost       = activeNavHost ?: return
        val navController = navHost.navController
        navHostListeners[navHost]?.let { oldListener ->
            navController.removeOnDestinationChangedListener(oldListener)
        }
        val newListener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (view != null) {
                binding?.headerTitle?.text = destination.label
                handleDestinationChange(destination.id)
                if (destination.id == R.id.homeFragment) {
                    wireHomeFragment()
                }
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
                    bottomNav.visibility           = View.GONE
                    icAddNotes.visibility          = View.INVISIBLE
                    ivMenu.visibility              = View.INVISIBLE
                    ivBack.visibility              = View.VISIBLE
                    ivCreateNote.visibility        = View.VISIBLE
                    // ✅ Only show kabab for existing saved notes (noteId != 0)
                    val isExistingNote = (createNotesViewModel.noteState.value?.noteId ?: 0L) != 0L
                    ivKabab.visibility             = if (isExistingNote) View.VISIBLE else View.GONE
                    ivDeleteAll.visibility         = View.GONE
                    setNoteHeader()
                }

                R.id.homeFragment -> {
                    ivCreateNote.visibility        = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility          = View.VISIBLE
                    bottomNav.visibility           = View.VISIBLE
                    ivMenu.visibility              = View.VISIBLE
                    ivBack.visibility              = View.INVISIBLE
                    ivKabab.visibility             = View.GONE
                    ivSavePremiumBadge.visibility  = View.GONE
                    ivDeleteAll.visibility         = View.GONE
                    setHomeTabChecked()
                    setDefaultNavHeader()
                }

                R.id.addTagsFragment -> {
                    ivCreateNote.visibility        = View.INVISIBLE
                    createNoteBottomBar.visibility = View.GONE
                    bottomNav.visibility           = View.GONE
                    icAddNotes.visibility          = View.GONE
                    ivMenu.visibility              = View.INVISIBLE
                    ivBack.visibility              = View.VISIBLE
                    ivKabab.visibility             = View.GONE
                    ivPremium.visibility           = View.GONE
                    ivSavePremiumBadge.visibility  = View.GONE
                    ivDeleteAll.visibility         = View.GONE
                    setTagsHeader()
                }

                R.id.previewFragment, R.id.previewFragment2 -> {
                    bottomNav.visibility          = View.GONE
                    ivKabab.visibility            = View.GONE
                    headerSave.visibility         = View.GONE
                    ivPremium.visibility          = View.GONE
                    ivRemainder.visibility        = View.GONE
                    ivMenu.visibility             = View.INVISIBLE
                    ivBack.visibility             = View.VISIBLE
                    ivSavePremiumBadge.visibility = View.GONE
                    icAddNotes.visibility         = View.GONE
                    ivDeleteAll.visibility        = View.GONE
                }

                R.id.remainderFragment -> {
                    bottomNav.visibility          = View.GONE
                    ivKabab.visibility            = View.GONE
                    headerSave.visibility         = View.GONE
                    ivRemainder.visibility        = View.GONE
                    ivMenu.visibility             = View.INVISIBLE
                    ivBack.visibility             = View.VISIBLE
                    ivPremium.visibility          = View.GONE
                    icAddNotes.visibility         = View.GONE
                    ivSavePremiumBadge.visibility = View.GONE
                    ivDeleteAll.visibility        = View.GONE
                }

                R.id.draftNotesFragment -> {
                    ivCreateNote.visibility        = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility          = View.INVISIBLE
                    bottomNav.visibility           = View.GONE
                    ivMenu.visibility              = View.GONE
                    ivBack.visibility              = View.GONE
                    ivKabab.visibility             = View.GONE
                    headerSave.visibility          = View.GONE
                    ivPremium.visibility           = View.GONE
                    ivSavePremiumBadge.visibility  = View.GONE
                    ivRemainder.visibility         = View.GONE
                    ivDeleteAll.visibility         = View.GONE
                    headerTitle.text               = ""
                }

                else -> {
                    ivCreateNote.visibility        = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility          = View.INVISIBLE
                    bottomNav.visibility           = View.VISIBLE
                    ivMenu.visibility              = View.INVISIBLE
                    ivBack.visibility              = View.VISIBLE
                    ivKabab.visibility             = View.GONE
                    ivSavePremiumBadge.visibility  = View.GONE
                    ivRemainder.visibility         = View.GONE
                    ivDeleteAll.visibility         = View.GONE
                }
            }
        }
    }

    private fun setHomeTabChecked() {
        if (binding?.bottomNav?.checkedButtonId != R.id.btnHome) {
            binding?.bottomNav?.check(R.id.btnHome)
            binding?.ivSavePremiumBadge?.visibility = View.GONE
        }
    }

    private fun setClickListeners() {
        binding?.apply {
            ivMenu.setOnClickListener {
                logAnalyticsEvent("Drawer_Button", "icon_click")
                parentLayout.openDrawer(GravityCompat.START)
            }
            ivBack.setOnClickListener {
                val dispatcher = requireActivity().onBackPressedDispatcher
                if (dispatcher.hasEnabledCallbacks()) {
                    dispatcher.onBackPressed()
                } else {
                    onBackTriggered()
                }
            }
            ivDeleteAll.setOnClickListener {
                getHomeFragment()?.deleteSelectedNotes()
            }
            ivRemainder.setOnClickListener {
                logAnalyticsEvent("Home_Screen_Reminder", "icon_click")
                viewModelNative.loadNativeAd(NativeAdKey.REMINDER_INTERVAL)
                onRemainderClick()
            }
            ivPremium.setOnClickListener {
                logAnalyticsEvent("Home_Screen_Premium_Icon", "icon_click")
                captureInnerDestBeforePaywall()
                proAccessManager.onPremiumIconClicked()
            }
            icAddNotes.setOnClickListener {
                preLoadNextAd(BannerAdKey.ADD_TASK)
                logAnalyticsEvent("Home_Screen_Add_Note", "button_click")
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                activeNavHost?.findNavController()?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId      = R.id.action_homeFragment_to_createNotesFragment2,
                    Bundle().apply { putBoolean(FROM_SCREEN, true) }
                )
            }
        }
    }

    private fun preLoadNextAd(adKey: BannerAdKey) {
        Log.d("AdsInformation", "NameFragment pre-loading ad for: ${adKey.value}")
        val adView = com.google.android.gms.ads.AdView(requireContext())
        bannerViewModel.loadBannerAd(adView, adKey, requireContext())
    }

    private fun logAnalyticsEvent(eventName: String, label: String) {
        if (eventName.isEmpty()) return
        mFirebaseAnalytics.logEvent(eventName, Bundle().apply {
            putString("action_label", label)
        })
    }

    private fun showCreateNoteExitAd() {
        sessionManagerRepo.bypassSecurityLogin(true)
        interstitialAdsConfig.showInterstitialWithDialog(
            requireActivity(),
            InterAdKey.ADD_TASK_INTER_BACKPRESS,
            object : InterstitialOnShowCallBack {
                override fun onAdDismissedFullScreenContent() {
                    sessionManagerRepo.bypassSecurityLogin(true)
                    checkRemoveAdsPopupOrNavigate()
                }
                override fun onAdFailedToShow() {
                    sessionManagerRepo.bypassSecurityLogin(true)
                    checkRemoveAdsPopupOrNavigate()
                }
                override fun onAdImpressionDelayed() {
                    sessionManagerRepo.bypassSecurityLogin(true)
                    checkRemoveAdsPopupOrNavigate()
                }
            }
        )
    }

    fun navigateInnerNavToHome() {
        fun doNavigate() {
            try {
                val innerNav = homeHost?.findNavController() ?: return
                innerNav.popBackStack(R.id.homeFragment, false)
            } catch (e: Exception) {
                android.util.Log.e("navigateInnerNavToHome", "popBackStack failed: ${e.message}")
            }
        }
        val fm = activity?.supportFragmentManager
        if (fm != null && fm.isStateSaved) {
            viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    doNavigate()
                }
            })
        } else {
            doNavigate()
        }
    }

    fun navigateBackToDraft() {
        fun doNavigate() {
            try {
                val innerNav = homeHost?.findNavController() ?: return
                if (innerNav.currentDestination?.id == R.id.createNotesFragment2 ||
                    innerNav.currentDestination?.id == R.id.createNotesFragment) {
                    innerNav.popBackStack()
                }
            } catch (e: Exception) {
                android.util.Log.e("navigateBackToDraft", "inner pop failed: ${e.message}")
            }
            try {
                findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId      = R.id.action_mainFragment_to_draftNotesFragment
                )
            } catch (e: Exception) {
                android.util.Log.e("navigateBackToDraft", "outer nav failed: ${e.message}")
            }
        }
        val fm = activity?.supportFragmentManager
        if (fm != null && fm.isStateSaved) {
            viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    doNavigate()
                }
            })
        } else {
            doNavigate()
        }
    }

    fun navigateInnerNavToCreateNote(draft: android.os.Bundle? = null) {
        fun doNavigate() {
            try {
                val innerNav    = homeHost.findNavController()
                val currentDest = innerNav.currentDestination?.id
                if (currentDest == R.id.homeFragment) {
                    innerNav.safeNav(
                        currentDestId = R.id.homeFragment,
                        actionId      = R.id.action_homeFragment_to_createNotesFragment2,
                        bundle        = draft
                    )
                } else {
                    innerNav.navigate(R.id.createNotesFragment2, draft)
                }
            } catch (e: Exception) {
                android.util.Log.e("navigateInnerNav", "navigateInnerNavToCreateNote failed: ${e.message}")
            }
        }
        val fm = activity?.supportFragmentManager
        if (fm != null && fm.isStateSaved) {
            viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    owner.lifecycle.removeObserver(this)
                    doNavigate()
                }
            })
        } else {
            doNavigate()
        }
    }

    private fun checkRemoveAdsPopupOrNavigate() {
        val shouldShow = sharedPref.shouldShowRemoveAdsPopup()
        if (shouldShow) {
            proAccessManager.onInterstitialCrossClicked(
                fragmentManager = requireActivity().supportFragmentManager,
                onAfterDismiss  = { activity?.runOnUiThread { navigateInnerNavToHome() } }
            )
        } else {
            navigateInnerNavToHome()
        }
    }

    private fun onBackTriggered() {
        val homeFragment = getHomeFragment()
        if (homeFragment?.isSelectionMode == true) {
            homeFragment.exitSelectionMode()
            return
        }

        val currentHost  = activeNavHost ?: return
        navController    = currentHost.findNavController()
        val currentDestId = navController.currentDestination?.id

        when (currentDestId) {
            R.id.addTagsFragment -> {
                navController.navigateUp()
            }
            R.id.createNotesFragment -> {
                showCreateNoteExitAd()
            }
            else -> {
                if (navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    handleBottomNavBackstack()
                }
            }
        }
    }

    private fun handleBottomNavBackstack() {
        if (view != null && binding?.parentLayout?.isDrawerOpen(GravityCompat.START) == true) {
            binding?.parentLayout?.closeDrawer(GravityCompat.START)
            return
        }

        if (binding?.bottomNav?.checkedButtonId != R.id.btnHome) {
            if (backStack.size > 1) {
                backStack.pop()
                val previousTabId = backStack.peek()
                if (view != null && previousTabId != null) {
                    binding?.bottomNav?.check(previousTabId)
                }
            } else {
                binding?.bottomNav?.check(R.id.btnHome)
            }
        } else {
            val homeNavController = homeHost.findNavController()
            if (homeNavController.previousBackStackEntry != null) {
                homeNavController.popBackStack()
            } else {
                showExitPopupFromConfig()
            }
        }
    }

    private fun setupBgTheme() {
        val currentTheme = sessionManagerRepo.getBgTheme()
        binding?.parentLayout?.loadBackground(
            resourceId  = currentTheme,
            placeholder = R.drawable.theme_1
        )
        applyDynamicTheme(currentTheme)
    }
    private fun setNoteHeader() {
        binding?.apply {
            headerTitle.text       = ContextCompat.getString(context ?: return, R.string.add_note)
            ivRemainder.visibility = View.GONE
            headerSave.visibility  = View.VISIBLE
            ivPremium.visibility   = View.GONE
            refreshPremiumBadges()

            // ── Save ─────────────────────────────────────────────────────────────
            headerSave.setOnClickListener {
                val noteFragment = activeNavHost?.childFragmentManager
                    ?.fragments?.find { it is CreateNotesFragment } as? CreateNotesFragment
                if (noteFragment?.getTitleText().isNullOrEmpty()) {
                    android.widget.Toast.makeText(
                        context,
                        getString(R.string.required_title),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                captureInnerDestBeforePaywall()
                proAccessManager.checkSaveNoteAccess(
                    fragmentManager = requireActivity().supportFragmentManager,
                    fromCreateNote  = true,
                    onWatchAd = {
                        innerDestBeforePaywall = null
                        getCreateNotesFragment()?.paywallCurrentlyOpen = false
                        pendingRewardGate = "save"
                        loadAndShowReward(RewardedAdKey.VIDEO_PRO_SAVE)
                    },
                    onAllowed = {
                        innerDestBeforePaywall = null
                        getCreateNotesFragment()?.paywallCurrentlyOpen = false
                        createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
                    },
                    onSubscribe = {
                        findNavController().safeNav(
                            currentDestId = R.id.mainFragment,
                            actionId      = R.id.action_global_mainPaywallFragment,
                            bundle        = com.example.easydiarysatti.paywalls.MainPaywallFragment.args(
                                fromCreateNote   = true,
                                fromRewardedGate = true
                            )
                        )
                    }
                )
            }
            ivKabab.setOnClickListener {
                val currentNote = createNotesViewModel.noteState.value
                val noteFragment = activeNavHost?.childFragmentManager
                    ?.fragments?.find { it is CreateNotesFragment } as? CreateNotesFragment

                // Use the updated .show() which takes the 'note' object
                CreateNoteOptionsBottomSheet.show(
                    fm = childFragmentManager,
                    note = currentNote, // Pass the whole object here
                    onFavorite = {clickedNote ->
                        createNotesViewModel.toggleFavorite(clickedNote)
                    },
                    onShare = {
                        noteFragment?.shareNote()
                    },
                    onDelete = {
                        noteFragment?.deleteNote()
                    }
                )
            }
            // ── ⋮ Kabab → CreateNoteOptionsBottomSheet ───────────────────────────

        }
    }


    private fun setProfileHeader() {
        binding?.apply {
            headerTitle.text       = ContextCompat.getString(context ?: return, R.string.edit_profile)
            ivRemainder.visibility = View.GONE
            headerSave.visibility  = View.VISIBLE
        }
    }

    private fun setTagsHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.tags)

//            ivRemainder.setImageResource(R.drawable.ic_search)
//            ivRemainder.visibility = View.VISIBLE
//            ivRemainder.setOnClickListener {
//                // Inner nav only — AddTagsFragment is a child of the active NavHost
//                val addTagsFrag = activeNavHost?.childFragmentManager?.fragments
//                    ?.find { it is AddTagsFragment } as? AddTagsFragment
//                addTagsFrag?.toggleSearchMode()
//            }
            ivRemainder.visibility = View.GONE
            headerSave.visibility         = View.GONE
            ivPremium.visibility          = View.GONE
            ivSavePremiumBadge.visibility = View.GONE
            ivDeleteAll.visibility        = View.GONE
        }
    }

    private fun setDefaultNavHeader() {
        binding?.apply {
            ivMenu.setImageResource(R.drawable.ic_menu)
            ivRemainder.visibility        = View.VISIBLE
            headerSave.visibility         = View.GONE
            ivSavePremiumBadge.visibility = View.GONE
            ivPremium.setImageResource(R.drawable.ic_premium)
            ivPremium.visibility = if (sharedPref.isAppPurchased) View.GONE else View.VISIBLE
            ivRemainder.setImageResource(R.drawable.notification)
            ivSavePremiumBadge.visibility = View.GONE

            val showMediaPremium = !sharedPref.isAppPurchased && sharedPref.shouldShowRewardedForMedia()
            btnMedia.icon = ContextCompat.getDrawable(
                requireContext(),
                if (showMediaPremium) R.drawable.media_premium_ic else R.drawable.media_ic
            )
            btnMedia.iconTint = if (showMediaPremium) null
            else ContextCompat.getColorStateList(requireContext(), R.color.selector_create_note_item2)
        }
    }

    fun observeMainState() {
        viewLifecycleOwner.lifecycleScope.launch {
            createNotesViewModel.noteState.flowWithLifecycle(
                viewLifecycleOwner.lifecycle,
                androidx.lifecycle.Lifecycle.State.CREATED
            ).collect { it ->
                val iv = binding?.ivCreateNote ?: return@collect
                if (view == null) return@collect

                // ✅ Sync kabab visibility when noteId changes (e.g. after first save)
                val onCreateNoteScreen =
                    activeNavHost?.navController?.currentDestination?.id == R.id.createNotesFragment
                if (onCreateNoteScreen) {
                    val isExistingNote = (it?.noteId ?: 0L) != 0L
                    binding?.ivKabab?.visibility = if (isExistingNote) View.VISIBLE else View.GONE
                }

                val uriString  = it?.bgImageUri
                val resourceId = it?.backgroundRes
                when {
                    !uriString.isNullOrEmpty() -> {
                        com.bumptech.glide.Glide.with(iv.context)
                            .load(android.net.Uri.parse(uriString))
                            .override(
                                iv.width.takeIf  { w -> w > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                                iv.height.takeIf { h -> h > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
                            )
                            .centerCrop().into(iv)
                    }
                    resourceId != null && resourceId != 0 -> {
                        com.bumptech.glide.Glide.with(iv.context)
                            .load(resourceId)
                            .override(
                                iv.width.takeIf  { w -> w > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                                iv.height.takeIf { h -> h > 0 } ?: com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
                            )
                            .centerCrop().into(iv)
                    }
                    else -> {
                        com.bumptech.glide.Glide.with(iv.context).clear(iv)
                        iv.setImageDrawable(null)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val state      = createNotesViewModel.noteState.value ?: return
        val iv         = binding?.ivCreateNote ?: return
        val uriString  = state.bgImageUri
        val resourceId = state.backgroundRes
        when {
            !uriString.isNullOrEmpty() -> {
                com.bumptech.glide.Glide.with(iv.context)
                    .load(android.net.Uri.parse(uriString))
                    .centerCrop().into(iv)
            }
            resourceId != null && resourceId != 0 -> {
                com.bumptech.glide.Glide.with(iv.context)
                    .load(resourceId)
                    .centerCrop().into(iv)
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
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.POST_NOTIFICATIONS
        } else null

        val proceedToReminder: () -> Unit = {
            (activity as? MainActivity)?.requestExactAlarmPermission {
                activeNavHost?.findNavController()?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId      = R.id.action_homeFragment_to_remainderFragment
                )
            }
        }

        if (permission != null &&
            ContextCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderAction = proceedToReminder
            notificationPermissionLauncher.launch(permission)
        } else {
            proceedToReminder()
        }
    }

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                createNotesViewModel.sendAction(
                    action = CreateNotesState.ImagePicked(imageUri = uri)
                )
            }
            AppLogger.createLog("MultiPicker", "Sent ${uris.size} images to ViewModel")
        }
        if (view != null) {
            binding?.bottomNavCreateNote?.clearChecked()
        }
    }

    private fun pickImage() {
        val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        pickPhotDialog(
            sessionManagerRepo = sessionManagerRepo,
            cameraCallBack = {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    logAnalyticsEvent("Add_Note_Take_a_Photo", "media_source")
                    sessionManagerRepo.bypassSecurityLogin(true)
                    imagePicker.pickFromCameraWithPermission()
                } else {
                    pendingPermissionSource = "camera"
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            galleryCallBack = {
                if (ContextCompat.checkSelfPermission(requireContext(), galleryPermission) == PackageManager.PERMISSION_GRANTED) {
                    logAnalyticsEvent("Add_Note_Upload_From_Gallery", "media_source")
                    pickMultipleMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    pendingPermissionSource = "gallery"
                    requestPermissionLauncher.launch(galleryPermission)
                }
            },
            onDismiss = {
                if (view != null) binding?.bottomNavCreateNote?.clearChecked()
            }
        )
    }

    private var pendingPermissionSource: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            when (pendingPermissionSource) {
                "gallery" -> {
                    logAnalyticsEvent("Add_Note_Upload_From_Gallery", "media_source")
                    pickMultipleMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                "camera" -> {
                    logAnalyticsEvent("Add_Note_Take_a_Photo", "media_source")
                    sessionManagerRepo.bypassSecurityLogin(true)
                    imagePicker.pickFromCameraWithPermission()
                }
                else -> pickImage()
            }
        }
        pendingPermissionSource = null
    }

    override fun onDestroyView() {
        stopProCardAutoScroll()
        billingManager.endConnection()
        super.onDestroyView()
        navHostListeners.forEach { (host, listener) ->
            host.navController.removeOnDestinationChangedListener(listener)
        }
        navHostListeners.clear()
    }

    // ── Rewarded Ad ──────────────────────────────────────────────────────────

    private fun loadAndShowReward(adKey: RewardedAdKey) {
        val gateKey = pendingRewardGate ?: run {
            android.util.Log.e("MainFragment", "loadAndShowReward: pendingRewardGate is null for $adKey")
            return
        }

        // ── Check remote config for loading dialog ────────────────────────────────
        val showLoadingDialog = sharedPref.getRewardedLoadingDialogShow(adKey.value)
        val loadingDialogTime = sharedPref.getRewardedLoadingDialogTime(adKey.value)

        // ── Show loading dialog if enabled ────────────────────────────────────────
        val loadingDialog: RewardedLoadingDialog? = if (showLoadingDialog && isAdded) {
            RewardedLoadingDialog.show(
                fm           = childFragmentManager,
                minDisplayMs = loadingDialogTime
            )
        } else null

        // ── Load the rewarded ad ──────────────────────────────────────────────────
        rewardedAdsConfig.loadRewardedAd(
            adType   = adKey,
            listener = object : RewardedOnLoadCallBack {
                override fun onResponse(isSuccess: Boolean) {
                    if (!isAdded) return
                    val act = activity ?: return

                    if (isSuccess) {
                        // Ad loaded — tell the dialog it can dismiss when ready,
                        // then show the ad in the callback
                        if (loadingDialog != null) {
                            loadingDialog.onAdReady {
                                // Called after dialog dismissed + min time elapsed
                                rewardedAdsConfig.showRewardedAd(
                                    act,
                                    adType   = adKey,
                                    listener = object : RewardedOnShowCallBack {
                                        override fun onAdFailedToShow()   = showRewardedInterstitialFailover(adKey, gateKey)
                                        override fun onUserEarnedReward() = onRewardGranted(gateKey)
                                    }
                                )
                            }
                        } else {
                            // No dialog — show ad directly (original behaviour)
                            rewardedAdsConfig.showRewardedAd(
                                act,
                                adType   = adKey,
                                listener = object : RewardedOnShowCallBack {
                                    override fun onAdFailedToShow()   = showRewardedInterstitialFailover(adKey, gateKey)
                                    override fun onUserEarnedReward() = onRewardGranted(gateKey)
                                }
                            )
                        }
                    } else {
                        // Ad failed to load — dismiss dialog immediately, run failover
                        if (loadingDialog != null) {
                            loadingDialog.onAdFailed {
                                showRewardedInterstitialFailover(adKey, gateKey)
                            }
                        } else {
                            showRewardedInterstitialFailover(adKey, gateKey)
                        }
                    }
                }
            }
        )
    }

    private fun showRewardedInterstitialFailover(adKey: RewardedAdKey, gateKey: String) {
        val failoverKey = when (adKey) {
            RewardedAdKey.VIDEO_PRO_BG    -> RewardedInterAdKey.REWARDED_INTER_FAILOVER_BG
            RewardedAdKey.VIDEO_PRO_MEDIA -> RewardedInterAdKey.REWARDED_INTER_FAILOVER_MEDIA
            RewardedAdKey.VIDEO_PRO_SAVE  -> RewardedInterAdKey.REWARDED_INTER_FAILOVER_SAVE
        }
        rewardedInterAdsConfig.loadRewardedInterAd(
            adType   = failoverKey,
            listener = object : RewardedOnLoadCallBack {
                override fun onResponse(isSuccess: Boolean) {
                    if (!isAdded) return
                    val act = activity ?: return
                    if (isSuccess) {
                        rewardedInterAdsConfig.showRewardedInterAd(
                            act,
                            adType   = failoverKey,
                            listener = object : RewardedOnShowCallBack {
                                override fun onAdFailedToShow()   = onRewardGranted(gateKey)
                                override fun onUserEarnedReward() = onRewardGranted(gateKey)
                            }
                        )
                    } else {
                        onRewardGranted(gateKey)
                    }
                }
            }
        )
    }

    private fun onRewardGranted(gateKey: String) {
        when (gateKey) {
            "save" -> proAccessManager.onSaveNoteRewardEarned {
                val noteFragment = activeNavHost?.childFragmentManager
                    ?.fragments?.find { it is CreateNotesFragment } as? CreateNotesFragment
                if (noteFragment?.getTitleText().isNullOrEmpty()) {
                    android.widget.Toast.makeText(
                        context,
                        getString(R.string.required_title),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@onSaveNoteRewardEarned
                }
                sessionManagerRepo.setRewardedJustShown(true)
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
                refreshPremiumBadges()
            }
            "media" -> proAccessManager.onMediaNoteRewardEarned {
                pickImage()
                refreshPremiumBadges()
            }
            "bg" -> {
                val src = pendingBgSource
                pendingBgSource = null
                activity?.runOnUiThread {
                    if (!isAdded || view == null) return@runOnUiThread
                    when (src) {
                        is com.example.easydiarysatti.utills.BgItem.DrawableRes -> {
                            applyBackground(src.resId)
                        }
                        is com.example.easydiarysatti.utills.BgItem.GalleryImage -> {
                            try {
                                requireContext().contentResolver.takePersistableUriPermission(
                                    src.uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("MainFragment", "takePersistableUriPermission failed: ${e.message}")
                            }
                            applyGalleryBackground(src.uri)
                        }
                        null -> Unit
                    }
                    refreshPremiumBadges()
                }
            }
        }
        pendingRewardGate = null
    }

    private fun refreshPremiumBadges() {
        if (!isAdded || view == null) return
        binding?.apply {
            val purchased       = sharedPref.isAppPurchased
            val showMediaPremium = !purchased && sharedPref.shouldShowRewardedForMedia()
            btnMedia.icon = ContextCompat.getDrawable(
                requireContext(),
                if (showMediaPremium) R.drawable.media_premium_ic else R.drawable.media_ic
            )
            btnMedia.iconTint = if (showMediaPremium) null
            else ContextCompat.getColorStateList(requireContext(), R.color.selector_create_note_item2)

            val onCreateNote = try {
                homeHost.findNavController().currentDestination?.id == R.id.createNotesFragment
            } catch (e: Exception) { false }
            val showSaveBadge = onCreateNote && !purchased && sharedPref.shouldShowRewardedForSave()
            ivSavePremiumBadge.visibility = if (showSaveBadge) View.VISIBLE else View.GONE
        }
    }

    private fun applyDynamicTheme(themeResId: Int?) {
        val themeColor = when (themeResId) {
            R.drawable.theme_1 -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
            R.drawable.theme_2 -> ContextCompat.getColor(requireContext(), R.color.theme2_color)
            R.drawable.theme_3 -> ContextCompat.getColor(requireContext(), R.color.theme3_color)
            R.drawable.theme_4 -> ContextCompat.getColor(requireContext(), R.color.theme4_color)
            R.drawable.theme_5 -> ContextCompat.getColor(requireContext(), R.color.theme5_color)
            else               -> ContextCompat.getColor(requireContext(), R.color.theme1_color)
        }

        binding?.apply {
            icAddNotes.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)

            val navButtons = listOf(btnHome, btnLibrary, btnCalendar)
            val navStates  = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val navColors  = intArrayOf(
                themeColor,
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            val navColors2 = intArrayOf(
                themeColor,
                ContextCompat.getColor(requireContext(), android.R.color.black)
            )
            val navSelector  = android.content.res.ColorStateList(navStates, navColors)
            val navSelector2 = android.content.res.ColorStateList(navStates, navColors2)

            navButtons.forEach { button ->
                button.backgroundTintList = navSelector
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    button.outlineSpotShadowColor    = themeColor
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