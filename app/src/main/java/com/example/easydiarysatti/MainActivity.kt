package com.example.easydiarysatti

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.example.easydiarysatti.domain.model.DrawerItem
import com.example.easydiarysatti.ui.onboarding.OnBoardingViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<OnBoardingViewModel>()

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

    private val colorPaletteSatti by lazy {
        listOf(
            "#334155".toColorInt(), // black-ish
            "#64748B".toColorInt(), // dark gray
            "#8478BF".toColorInt(), // light gray
            "#0F2A45".toColorInt(), // pink-ish
            "#0F172A".toColorInt(), // greenish
            "#4C0821".toColorInt()  // purple
        )
    }

    private val mainDrawerItemList: List<DrawerItem> by lazy {
        listOf(
            DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.pencil_icon,
                title = getString(R.string.edit_tags)
            ), DrawerItem(
                bgTint = "#5EE3A9",
                imgRes = R.drawable.paint_icon,
                title = getString(R.string.color_theme)
            ), DrawerItem(
                bgTint = "#FFDE8B",
                imgRes = R.drawable.bell_drawer,
                title = getString(R.string.remainders)
            ), DrawerItem(
                bgTint = "#FF8D95", imgRes = R.drawable.lock, title = getString(R.string.dairy_lock)
            ), DrawerItem(
                bgTint = "#A29DFB",
                imgRes = R.drawable.language_icon,
                title = getString(R.string.langauge)
            ), DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.share_icon,
                title = getString(R.string.share_app)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStartGraph()
    }

    fun getBgThemes(): List<Int?> = noteBgList
    fun getColorPalette(): List<Int>? = colorPaletteSatti
    fun getDrawerItemList(): List<DrawerItem> = mainDrawerItemList
    private fun setupStartGraph() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
            ?: return

        val navController = navHostFragment.navController
        val inflater = navController.navInflater
        val navGraph = inflater.inflate(R.navigation.mobile_navigation)
        if (viewModel.isOnBoardingCompleted()) {
            navGraph.setStartDestination(R.id.loginFragment)
        } else {
            navGraph.setStartDestination(R.id.onBoardingFragment)
        }
        navController.graph = navGraph

    }

    override fun onResume() {
        super.onResume()
        val isOnBoardingDone = viewModel.isOnBoardingCompleted()
        if (!isOnBoardingDone) return
        val cameraCall = viewModel.getCameraCall()
        Log.e("OnResumeApp-->", "onResume: $cameraCall")
        if (viewModel.shouldRequireLogin()) {
            viewModel.clearLogin()
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
                ?: return
            val navController = navHostFragment.navController
            if (navController.currentDestination?.id != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment)
            }
        }
    }

}