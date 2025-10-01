package com.example.easydiarysatti.ui.main

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentMainBinding
import com.example.easydiarysatti.databinding.FragmentThemesBinding
import com.example.easydiarysatti.databinding.FragmentThemesBinding.bind
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.viewBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment(R.layout.fragment_main) {
    private val viewModel by viewModels<MainViewModel>()
    private val binding by viewBinding(FragmentMainBinding::bind)

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavBar()
        setupBgTheme()
    }

    private fun setupBottomNavBar() {
        binding?.apply {
            val navHostFragment =
                childFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main2) as? NavHostFragment
            val navController = navHostFragment?.navController
            bottomNav.check(R.id.btnHome)
            bottomNav.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btnHome -> navHostFragment?.navController?.navigate(R.id.navigation_home)
                        R.id.btn_library -> navHostFragment?.navController?.navigate(R.id.navigation_dashboard)
                        R.id.btn_calendar -> navHostFragment?.navController?.navigate(R.id.navigation_notifications)
                    }
                }
            }
            navController?.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.navigation_createNote -> {
                        bottomNav.visibility = View.GONE
                        setNoteHeader()
                    }

                    else -> {
                        bottomNav.visibility = View.VISIBLE
                        destination.label?.toString()?.setDefaultNavHeader()
                    }
                }
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