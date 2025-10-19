package com.example.easydiarysatti.ui.main

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
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
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.safeNav
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
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private lateinit var calendarHost: NavHostFragment
    private lateinit var libraryHost: NavHostFragment
    private lateinit var homeHost: NavHostFragment
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val viewModel by activityViewModels<NameViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate
    private var activeNavHost: NavHostFragment? = null

    private val navHostListeners =
        mutableMapOf<NavHostFragment, NavController.OnDestinationChangedListener>()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    private val multiImageAdapter: MultiImageAdapter by lazy {
        MultiImageAdapter(items = (activity as MainActivity).getBgThemes(), onUploadClick = {
        }, onImageClick = {
            binding?.ivCreateNote?.loadImage(
                resourceId = it,
                placeholder = 0
            )
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
                0 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_addTagsFragment2,
                    bundle = Bundle().apply {
                        putBoolean(FROM_SCREEN, true)
                    }
                )

                1 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_themesFragment
                )

                2 -> {
                    binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    onRemainderClick()
                }

                3 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_changePasswordFragment
                )

                4 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_languageFragment
                )

                5 -> Unit
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = ImagePickerDelegate(
            this,
            onPickerClosed = {
                sessionManagerRepo.bypassSecurityLogin(false)
                binding?.bottomNavCreateNote?.clearChecked()
            },
            onImagePicked = { uri: Uri?, file: File? ->
                sessionManagerRepo.bypassSecurityLogin(false)
                showImageCropDialog(
                    imagePath = file?.path ?: return@ImagePickerDelegate,
                    btnDone = {
                        createNotesViewModel.sendAction(
                            action = CreateNotesState.ImagePicked(
                                imageUri = it
                            )
                        )
                    },
                    closeDialog = {
                        binding?.bottomNavCreateNote?.clearChecked()
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
        childFragmentManager.beginTransaction()
            .hide(libraryHost)
            .hide(calendarHost)
            .show(homeHost)
            .commitNow()
        activeNavHost = homeHost
        binding?.bottomNav?.clearChecked()
        setupNavControllerListener()
        setupBottomNav()
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
        setupDrawer()
        observeMainState()
    }

    private fun setupBottomNavBar() {
        binding?.apply {
            bottomNavCreateNote.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnBackground -> {
                            showBackgroundDialog(
                                adapterMultiImageAdapter = multiImageAdapter,
                                closeDialog = {
                                    binding?.bottomNavCreateNote?.clearChecked()
                                })
                        }

                        R.id.btn_hash_tag -> {
                            createNotesViewModel.sendAction(CreateNotesState.TagAction)
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(50)
                                group.clearChecked()
                            }
                        }

                        R.id.btn_media -> {
                            pickPhotDialog(cameraCallBack = {
                                sessionManagerRepo.bypassSecurityLogin(true)
                                imagePicker.pickFromCameraWithPermission()
                            }, galleryCallBack = {
                                imagePicker.pickFromGalleryWithPermission()
                            }, onDismiss = {
                                binding?.bottomNavCreateNote?.clearChecked()
                            })
                        }

                        R.id.btn_text -> {
                            showEditTexDialog(
                                closeDialog = {
                                    binding?.bottomNavCreateNote?.clearChecked()
                                },
                                fontSelectionListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.FontAction(it))
                                },
                                textAlignmentListener = {
                                    createNotesViewModel.sendAction(
                                        CreateNotesState.TextAlignment(
                                            it
                                        )
                                    )
                                },
                                textBoldListener = {
                                    createNotesViewModel.sendAction(CreateNotesState.HeadingSize(it))
                                },
                                textColorListener = {
                                    Log.e("SelecetdColor", "setTextColor: SelecetdColor$it")
                                    createNotesViewModel.sendAction(CreateNotesState.TextColor(it))
                                },
                                colorPalette = (activity as? MainActivity)?.getColorPalette()
                                    ?: listOf()
                            )
                        }
                    }
                }
            }
            bottomNav.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnHome -> {
                            binding?.icAddNotes?.visibility = View.VISIBLE
                            binding?.ivRemainder?.visibility = View.VISIBLE
                        }

                        R.id.btn_library -> {
                            binding?.icAddNotes?.visibility = View.GONE
                            binding?.ivRemainder?.visibility = View.GONE
                        }

                        R.id.btn_calendar -> {
                            binding?.icAddNotes?.visibility = View.GONE
                            binding?.ivRemainder?.visibility = View.GONE
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
                R.id.btnHome -> homeHost
                R.id.btn_library -> libraryHost
                R.id.btn_calendar -> calendarHost
                else -> return@addOnButtonCheckedListener
            }
            Log.e("Machood", "setupBottomNav: ${activeNavHost?.id}-->${targetHost.id}")
            if (targetHost == activeNavHost) {
                val navController = targetHost.navController
                val startDestinationId = navController.graph.startDestinationId
                navController.popBackStack(startDestinationId, false)
                Log.e("Machood", "setupBottomNav: ${targetHost.id}")
                return@addOnButtonCheckedListener
            }
            childFragmentManager.beginTransaction()
                .hide(activeNavHost ?: return@addOnButtonCheckedListener)
                .show(targetHost)
                .commitNowAllowingStateLoss()
            activeNavHost = targetHost
            setupNavControllerListener()
        }
    }

    private fun setupDrawer() {
        binding?.apply {
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
                if (!profilePic.isEmpty()) {
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
            Log.e("OnCHangeNaju", "setupBottomNavBar: ${destination.label}")
            binding?.headerTitle?.text = destination.label
            handleDestinationChange(destination.id)
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
                    binding?.ivKabab?.visibility = View.GONE
                    setNoteHeader()
                }

                R.id.homeFragment -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility = View.VISIBLE
                    bottomNav.visibility = View.VISIBLE
                    ivMenu.visibility = View.VISIBLE
                    ivBack.visibility = View.INVISIBLE
                    binding?.btnHome?.isChecked = true
                    binding?.ivKabab?.visibility = View.GONE
                    setDefaultNavHeader()
                }

                R.id.addTagsFragment -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.GONE
                    bottomNav.visibility = View.GONE
                    icAddNotes.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    binding?.ivKabab?.visibility = View.GONE
                    binding?.ivRemainder?.visibility = View.GONE
                    setTagsHeader()
                }

                R.id.previewFragment, R.id.previewFragment2 -> {
                    binding?.bottomNav?.visibility = View.GONE
                    binding?.ivKabab?.visibility = View.VISIBLE
                    binding?.headerSave?.visibility = View.GONE
                    binding?.ivRemainder?.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    icAddNotes.visibility = View.GONE
                }

                R.id.remainderFragment -> {
                    binding?.bottomNav?.visibility = View.GONE
                    binding?.ivKabab?.visibility = View.VISIBLE
                    binding?.headerSave?.visibility = View.GONE
                    binding?.ivRemainder?.visibility = View.GONE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    icAddNotes.visibility = View.GONE
                }

                else -> {
                    ivCreateNote.visibility = View.INVISIBLE
                    createNoteBottomBar.visibility = View.INVISIBLE
                    icAddNotes.visibility = View.INVISIBLE
                    bottomNav.visibility = View.VISIBLE
                    ivMenu.visibility = View.INVISIBLE
                    ivBack.visibility = View.VISIBLE
                    binding?.ivKabab?.visibility = View.GONE
                    binding?.ivRemainder?.visibility = View.GONE
                }
            }
        }
    }

    private fun setClickListeners() {
        binding?.apply {
            ivMenu.setOnClickListener {
                binding?.parentLayout?.openDrawer(GravityCompat.START)
            }
            ivBack.setOnClickListener {
                val currentDestId = activeNavHost?.findNavController()?.currentDestination?.id
                Log.d(
                    "CurrentDest",
                    "Current destination ID: $currentDestId--${activeNavHost?.findNavController()?.currentDestination?.label}"
                )
                when (currentDestId) {
                    R.id.addTagsFragment -> {
                        createNotesViewModel.sendAction(
                            action = CreateNotesState.AddTag(
                                tag = "Personal",
                                createNoteEntity = createNotesViewModel.noteState.value
                            )
                        )
                        activeNavHost?.findNavController()?.navigateUp()
                    }

                    else -> {
                        val currentHost = activeNavHost ?: return@setOnClickListener
                        if (currentHost != homeHost) {
                            binding?.bottomNav?.check(R.id.btnHome)
                            return@setOnClickListener
                        }
                        val homeNavController = homeHost.navController
                        if (homeNavController.currentDestination?.id != R.id.homeFragment &&
                            homeNavController.popBackStack()
                        ) {
                            return@setOnClickListener
                        }
                    }
                }
            }
            headerSave.setOnClickListener {
                Log.e("headerSave", "setClickListeners: ")
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
            ivRemainder.setOnClickListener {
                onRemainderClick()
            }
            icAddNotes.setOnClickListener {
                createNotesViewModel.clearTags()
                createNotesViewModel.clearImages()
                createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                activeNavHost?.findNavController()?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId = R.id.action_homeFragment_to_createNotesFragment2,
                    Bundle().apply {
                        putBoolean(FROM_SCREEN, true)
                    }
                )
            }
        }
    }

    private fun setupBgTheme() {
        binding?.parentLayout?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(),
            placeholder = R.drawable.theme_1
        )
    }

    private fun setNoteHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.add_note)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.VISIBLE
        }
    }

    private fun setTagsHeader() {
        binding?.apply {
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.tags)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.GONE
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
                binding?.ivCreateNote?.loadImage(
                    resourceId = it?.backgroundRes,
                    placeholder = 0
                )
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentHost = activeNavHost ?: return
                if (currentHost != homeHost) {
                    binding?.bottomNav?.check(R.id.btnHome)
                    return
                }
                val homeNavController = homeHost.navController
                if (homeNavController.currentDestination?.id != R.id.homeFragment &&
                    homeNavController.popBackStack()
                ) {
                    return
                }
                if (binding?.parentLayout?.isDrawerOpen(GravityCompat.START) == true) {
                    binding?.parentLayout?.closeDrawer(GravityCompat.START)
                    return
                }
                showFeedBackDialog {
                    activity?.finish()
                }
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


    override fun onDestroyView() {
        super.onDestroyView()
        navHostListeners.forEach { (host, listener) ->
            host.navController.removeOnDestinationChangedListener(listener)
        }
        navHostListeners.clear()
    }

}