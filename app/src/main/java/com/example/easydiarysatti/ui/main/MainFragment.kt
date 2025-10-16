package com.example.easydiarysatti.ui.main

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.MainActivity
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.isNotificationEnabled
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.loadImage
import com.example.easydiarysatti.notificationPermission
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.utills.ImagePickerDelegate
import com.example.easydiarysatti.utills.MultiImageAdapter
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
    private val mainViewModel by activityViewModels<MainViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate

    private var activeNavHost: NavHostFragment? = null

    private val colorPalette by lazy {
        listOf(
            "#334155".toColorInt(), // black-ish
            "#64748B".toColorInt(), // dark gray
            "#8478BF".toColorInt(), // light gray
            "#0F2A45".toColorInt(), // pink-ish
            "#0F172A".toColorInt(), // greenish
            "#4C0821".toColorInt()  // purple
        )
    }

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
                    actionId = R.id.action_mainFragment_to_editTagsFragment
                )

                1 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_themesFragment
                )

                2 -> Unit
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
                binding?.bottomNavCreateNote?.clearChecked()
            },
            onImagePicked = { uri: Uri?, file: File? ->
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
        setupBottomNav()
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
        setupDrawer()
        observeMainState()


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
            if (targetHost == activeNavHost) return@addOnButtonCheckedListener
            childFragmentManager.beginTransaction()
                .hide(activeNavHost ?: return@addOnButtonCheckedListener)
                .show(targetHost)
                .commitNowAllowingStateLoss()
            activeNavHost = targetHost
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

    private fun setupBottomNavBar() {
        binding?.apply {
            binding?.bottomNavCreateNote?.clearChecked()
            activeNavHost?.findNavController()
                ?.addOnDestinationChangedListener { _, destination, _ ->
                    when (destination.id) {
                        R.id.createNotesFragment -> {
                            createNoteBottomBar.visibility = View.VISIBLE
                            bottomNav.visibility = View.INVISIBLE
                            binding?.icAddNotes?.visibility = View.INVISIBLE
                            ivMenu.visibility = View.INVISIBLE
                            ivBack.visibility = View.VISIBLE

                            binding?.ivCreateNote?.visibility = View.VISIBLE

                            setNoteHeader()
                        }

                        R.id.homeFragment -> {

                            binding?.ivCreateNote?.visibility = View.INVISIBLE

                            createNoteBottomBar.visibility = View.INVISIBLE
                            binding?.icAddNotes?.visibility = View.VISIBLE
                            bottomNav.visibility = View.VISIBLE
                            ivMenu.visibility = View.VISIBLE
                            ivBack.visibility = View.INVISIBLE
                            destination.label?.toString()?.setDefaultNavHeader()
                            binding?.bottomNav?.check(R.id.btnHome)
                        }

                        R.id.addTagsFragment -> {
                            binding?.ivCreateNote?.visibility = View.INVISIBLE
                            createNoteBottomBar.visibility = View.GONE
                            bottomNav.visibility = View.GONE
                            binding?.icAddNotes?.visibility = View.GONE
                            ivMenu.visibility = View.INVISIBLE
                            ivBack.visibility = View.VISIBLE
                            setTagsHeader()
                        }

                        else -> {
                            binding?.ivCreateNote?.visibility = View.INVISIBLE
                            createNoteBottomBar.visibility = View.INVISIBLE
                            binding?.icAddNotes?.visibility = View.INVISIBLE
                            bottomNav.visibility = View.VISIBLE
                            ivMenu.visibility = View.INVISIBLE
                            ivBack.visibility = View.VISIBLE
                            destination.label?.toString()?.setDefaultNavHeader()
                            val label = destination.label?.toString()
                            Log.d("NavDebug", "Navigated to: $label")
                            label?.setDefaultNavHeader()
                        }
                    }
                }

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
                            imagePicker.showPickerDialog()
                        }

                        R.id.btn_text -> {
                            showEditTexDialog(
                                closeDialog = {
                                    binding?.bottomNavCreateNote?.clearChecked()
                                }, fontSelectionListener = {
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
                                }, colorPalette = colorPalette
                            )
                        }
                    }
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
                Log.d("CurrentDest", "Current destination ID: $currentDestId")
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

                    else -> activeNavHost?.findNavController()?.navigateUp()
                }
            }
            headerSave.setOnClickListener {
                Log.e("headerSave", "setClickListeners: ")
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
            ivRemainder.setOnClickListener {
                val alarmManager =
                    requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        startActivity(intent)
                    }
                    return@setOnClickListener
                }
                if (activity?.isNotificationEnabled() == true) {
                    activeNavHost?.findNavController()?.safeNav(
                        currentDestId = R.id.homeFragment,
                        actionId = R.id.action_homeFragment_to_remainderFragment
                    )
                } else {
                    activity.notificationPermission()
                }
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

    private fun String.setDefaultNavHeader() {
        binding?.apply {
            ivMenu.setImageResource(R.drawable.ic_menu)
            headerTitle.text = this@setDefaultNavHeader
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
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

}