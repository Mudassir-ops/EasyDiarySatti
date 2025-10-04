package com.example.easydiarysatti.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.createnote.CreateNotesState
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.utills.ImagePickerDelegate
import com.example.easydiarysatti.utills.showImageCropDialog
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private var innerNavController: NavController? = null
    private val createNotesViewModel by activityViewModels<CreateNotesViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)
    private lateinit var imagePicker: ImagePickerDelegate

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = ImagePickerDelegate(this) { uri, file ->
            showImageCropDialog(imagePath = file?.path ?: return@ImagePickerDelegate, btnDone = {
                createNotesViewModel.sendAction(action = CreateNotesState.ImagePicked(imageUri = it))
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavBar()
        setupBgTheme()
        setClickListeners()
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
                        binding?.icAddNotes?.visibility = View.GONE
                        setNoteHeader()
                    }

                    R.id.homeFragment -> {
                        createNoteBottomBar.visibility = View.GONE
                        binding?.icAddNotes?.visibility = View.VISIBLE
                        bottomNav.visibility = View.VISIBLE
                        setTagsHeader()
                    }

                    R.id.addTagsFragment2 -> {
                        createNoteBottomBar.visibility = View.GONE
                        binding?.icAddNotes?.visibility = View.GONE
                        bottomNav.visibility = View.GONE
                        destination.label?.toString()?.setDefaultNavHeader()
                    }

                    else -> {
                        createNoteBottomBar.visibility = View.GONE
                        binding?.icAddNotes?.visibility = View.GONE
                        bottomNav.visibility = View.VISIBLE
                        destination.label?.toString()?.setDefaultNavHeader()
                    }
                }
            }
            bottomNavCreateNote.addOnButtonCheckedListener { group, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnBackground -> Unit
                        R.id.btn_hash_tag -> innerNavController?.safeNav(
                            currentDestId = R.id.createNotesFragment,
                            actionId = R.id.action_createNotesFragment_to_addTagsFragment2
                        )

                        R.id.btn_media -> imagePicker.showPickerDialog()
                        R.id.btn_text -> Unit
                    }
                }
            }
        }
    }

    private fun setClickListeners() {
        binding?.apply {
            ivMenu.setOnClickListener { createNotesViewModel.sendAction(action = CreateNotesState.BackAction) }
            headerSave.setOnClickListener {
                Log.e("headerSave", "setClickListeners: ")
                createNotesViewModel.sendAction(action = CreateNotesState.SaveNote)
            }
            icAddNotes.setOnClickListener {
                Log.e(
                    "setClickListeners",
                    "setClickListeners: ${innerNavController?.currentDestination?.label}",
                )
                createNotesViewModel.setCurrentNoteId(noteId = -1)
                innerNavController?.safeNav(
                    currentDestId = R.id.homeFragment,
                    actionId = R.id.action_homeFragment_to_createNotesFragment
                )
            }
        }
    }

    private fun setupBgTheme() {
        binding?.parentLayout?.loadBackground(
            resourceId = sessionManagerRepo.getBgTheme(), placeholder = R.drawable.theme_1
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
            headerSave.visibility = View.VISIBLE
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