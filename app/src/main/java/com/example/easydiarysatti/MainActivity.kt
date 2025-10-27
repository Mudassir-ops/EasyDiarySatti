package com.example.easydiarysatti

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.easydiarysatti.data.repo.UpdateState
import com.example.easydiarysatti.databinding.ActivityMainBinding
import com.example.easydiarysatti.domain.model.DrawerItem
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.onboarding.OnBoardingViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val updateViewModel: UpdateViewModel by viewModels()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo

    private val viewModel by viewModels<OnBoardingViewModel>()

    private var onAllPermissionsGranted: (() -> Unit)? = null

    private val exactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            sessionManagerRepo.bypassSecurityLogin(false)
            val alarmManager =
                getSystemService(ALARM_SERVICE) as? AlarmManager ?: return@registerForActivityResult
            if (AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
                checkAndRequestNotificationPermission(onAllPermissionsGranted)
            } else {
                binding.main.showSnackbar(getString(R.string.you_must_allow_schedule_alarm_permission))
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            sessionManagerRepo.bypassSecurityLogin(false)
            if (isGranted) {
                onAllPermissionsGranted?.invoke()
                onAllPermissionsGranted = null
            } else {
                binding.main.showSnackbar(getString(R.string.you_must_allow_notification_permission))
                onAllPermissionsGranted = null
            }
        }

    fun requestExactAlarmPermission(onAllGranted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager ?: return
            if (!AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
                sessionManagerRepo.bypassSecurityLogin(true)
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                exactAlarmPermissionLauncher.launch(intent)
                this.onAllPermissionsGranted = onAllGranted
            } else {
                sessionManagerRepo.bypassSecurityLogin(false)
                checkAndRequestNotificationPermission(onAllGranted)
            }
        } else {
            sessionManagerRepo.bypassSecurityLogin(false)
            checkAndRequestNotificationPermission(onAllGranted)
        }
    }

    fun checkAndRequestNotificationPermission(onAllGranted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(this)
            if (!notificationManager.areNotificationsEnabled()) {
                sessionManagerRepo.bypassSecurityLogin(true)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                this.onAllPermissionsGranted = onAllGranted
            } else {
                sessionManagerRepo.bypassSecurityLogin(false)
                onAllGranted?.invoke()
            }
        } else {
            sessionManagerRepo.bypassSecurityLogin(false)
            onAllGranted?.invoke()
        }
    }

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
                imgRes = R.drawable.ic_privacy_policy,
                title = getString(R.string.privacy_policy)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStartGraph()
        checkAutoUpdate()
    }

    fun getBgThemes(): List<Int?> = noteBgList
    fun getColorPalette(): List<Int>? = colorPaletteSatti
    fun getDrawerItemList(): List<DrawerItem> = mainDrawerItemList
    private fun setupStartGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
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
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
                    ?: return
            val navController = navHostFragment.navController
            if (navController.currentDestination?.id != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment)
            }
        }
        updateViewModel.checkDownloadedOnResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateViewModel.unregisterListener()
    }

    private fun checkAutoUpdate() {
        updateViewModel.init(this)
        updateViewModel.checkForUpdates()
        lifecycleScope.launch {
            updateViewModel.updateState.collect {
                when (it) {
                    is UpdateState.Downloaded -> {
                        showRestartSnackBar()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showRestartSnackBar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Update ready",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Restart") {
                updateViewModel.completeUpdate()
            }.show()
    }

}