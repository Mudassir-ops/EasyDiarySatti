package com.example.easydiarysatti.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.model.DrawerItem
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.utills.ImagePickerDelegate
import com.example.easydiarysatti.utills.MultiImageAdapter
import com.example.easydiarysatti.utills.setImage
import com.example.easydiarysatti.utills.showBackgroundDialog
import com.example.easydiarysatti.utills.showEditTexDialog
import com.example.easydiarysatti.utills.showImageCropDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private var innerNavController: NavController? = null
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val viewModel by activityViewModels<NameViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate
    private val drawerItemList: List<DrawerItem> by lazy {
        listOf(
            DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.pencil_icon,
                title = getString(R.string.edit_tags)
            ),
            DrawerItem(
                bgTint = "#5EE3A9",
                imgRes = R.drawable.paint_icon,
                title = getString(R.string.color_theme)
            ),
            DrawerItem(
                bgTint = "#FFDE8B",
                imgRes = R.drawable.bell_drawer,
                title = getString(R.string.remainders)
            ),
            DrawerItem(
                bgTint = "#FF8D95",
                imgRes = R.drawable.lock,
                title = getString(R.string.dairy_lock)
            ),
            DrawerItem(
                bgTint = "#A29DFB",
                imgRes = R.drawable.language_icon,
                title = getString(R.string.langauge)
            ),
            DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.share_icon,
                title = getString(R.string.share_app)
            )
        )
    }

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    private val noteBgList: List<Int?> by lazy {
        listOf(
            null,
            R.drawable.note_bg_1,
            R.drawable.note_bg_2,
            R.drawable.note_bg_3,
            R.drawable.note_bg_4,
            R.drawable.note_bg_3,
        )
    }
    private val multiImageAdapter: MultiImageAdapter by lazy {
        MultiImageAdapter(items = noteBgList, onUploadClick = {
            val imagePicker = ImagePickerDelegate(this) { uri, file ->

            }
            imagePicker.showPickerDialog()
        }, onImageClick = {
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
                    actionId = R.id.action_mainFragment_to_addTagsFragment
                )

                1 -> findNavController().safeNav(
                    currentDestId = R.id.mainFragment,
                    actionId = R.id.action_mainFragment_to_themesFragment
                )

                2 -> Unit
                3 -> Unit
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
        imagePicker = ImagePickerDelegate(this) { uri, file ->
            showImageCropDialog(imagePath = file?.path ?: return@ImagePickerDelegate, btnDone = {
                createNotesViewModel.sendAction(action = CreateNotesState.ImagePicked(imageUri = it))
            }, closeDialog = {
                binding?.bottomNavCreateNote?.clearChecked()
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
        setupDrawer()
    }

    private fun setupDrawer() {
        binding?.apply {
            binding?.parentLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            drawerLayout.drawerItems.run {
                adapter = drawerItemAdapter
                hasFixedSize()
            }
            drawerItemAdapter.submitList(drawerItemList)
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
            val innerNavHost =
                childFragmentManager.findFragmentById(R.id.nav_host_main_inner) as NavHostFragment
            innerNavController = innerNavHost.navController
            bottomNav.check(R.id.btnHome)
            bottomNav.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnHome -> innerNavController?.navigate(R.id.homeFragment)
                        R.id.btn_library -> innerNavController?.navigate(R.id.libraryFragment)
                        R.id.btn_calendar -> innerNavController?.navigate(R.id.calenderFragment)
                    }
                }
            }
            innerNavController?.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.createNotesFragment -> {
                        createNoteBottomBar.visibility = View.VISIBLE
                        bottomNav.visibility = View.INVISIBLE
                        binding?.icAddNotes?.visibility = View.INVISIBLE
                        setNoteHeader()
                    }

                    R.id.homeFragment -> {
                        createNoteBottomBar.visibility = View.INVISIBLE
                        binding?.icAddNotes?.visibility = View.VISIBLE
                        bottomNav.visibility = View.VISIBLE
                        destination.label?.toString()?.setDefaultNavHeader()
                    }

                    R.id.addTagsFragment2 -> {
                        createNoteBottomBar.visibility = View.INVISIBLE
                        binding?.icAddNotes?.visibility = View.INVISIBLE
                        bottomNav.visibility = View.INVISIBLE
                        setTagsHeader()

                    }

                    else -> {
                        createNoteBottomBar.visibility = View.INVISIBLE
                        binding?.icAddNotes?.visibility = View.INVISIBLE
                        bottomNav.visibility = View.VISIBLE
                        destination.label?.toString()?.setDefaultNavHeader()
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
                                delay(200)
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
                                    createNotesViewModel.sendAction(CreateNotesState.TextColor(it))
                                }
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
                createNotesViewModel.sendAction(action = CreateNotesState.BackAction)
            }
            headerSave.setOnClickListener {
                Log.e("headerSave", "setClickListeners: ")
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
            icAddNotes.setOnClickListener {
                createNotesViewModel.setupNoteEntity(createNoteEntity = null)
                innerNavController?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId = R.id.action_homeFragment_to_createNotesFragment
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
            ivMenu.setImageResource(R.drawable.back_icon)
            headerTitle.text = ContextCompat.getString(context ?: return, R.string.add_note)
            ivRemainder.visibility = View.GONE
            headerSave.visibility = View.VISIBLE
        }
    }

    private fun setTagsHeader() {
        binding?.apply {
            ivMenu.setImageResource(R.drawable.back_icon)
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

}