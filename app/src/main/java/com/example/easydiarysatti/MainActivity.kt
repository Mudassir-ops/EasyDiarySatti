package com.example.easydiarysatti

import android.Manifest
import android.R.attr.required
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.easydiarysatti.ads.cmp.ConsentController
import com.example.easydiarysatti.ads.cmp.ConsentController.ConsentCallback
import com.example.easydiarysatti.data.repo.UpdateState
import com.example.easydiarysatti.databinding.ActivityMainBinding
import com.example.easydiarysatti.domain.model.DrawerItem
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.ui.login.LoginFragment
import com.example.easydiarysatti.ui.onboarding.OnBoardingViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ConsentCallback {

    private lateinit var binding: ActivityMainBinding
    private val updateViewModel: UpdateViewModel by viewModels()
    private lateinit var consentController: ConsentController

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

    private val noteBgList: MutableList<com.example.easydiarysatti.utills.BgItem?> by lazy {
        mutableListOf(
            null,
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_1),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_2),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_3),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_4),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_5),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_6),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_7),
            com.example.easydiarysatti.utills.BgItem.DrawableRes(R.drawable.background_8),
        )
    }

    private val colorPaletteSatti by lazy {
        listOf(
            "#334155".toColorInt(),
            "#64748B".toColorInt(),
            "#8478BF".toColorInt(),
            "#0F2A45".toColorInt(),
            "#0F172A".toColorInt(),
            "#4C0821".toColorInt()
        )
    }

    private val mainDrawerItemList: List<DrawerItem> by lazy {
        listOf(
            DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.pencil_icon,
                title  = getString(R.string.edit_tags2)
            ),
            DrawerItem(
                bgTint = "#E8BA00",
                imgRes = R.drawable.ic_file_favourite,
                title  = getString(R.string.favorites)
            ),
            DrawerItem(
                bgTint = "#42ABD0",
                imgRes = R.drawable.ic_draft_document1,
                title  = getString(R.string.draft)
            ),
            DrawerItem(
                bgTint = "#5EE3A9",
                imgRes = R.drawable.paint_icon,
                title  = getString(R.string.color_theme)
            ),
            DrawerItem(
                bgTint = "#FFDE8B",
                imgRes = R.drawable.bell_drawer,
                title  = getString(R.string.remainders)
            ),
            DrawerItem(
                bgTint = "#FF8D95",
                imgRes = R.drawable.lock,
                title  = getString(R.string.dairy_lock)
            ),
            DrawerItem(
                bgTint = "#A29DFB",
                imgRes = R.drawable.language_icon,
                title  = getString(R.string.langauge)
            ),
            DrawerItem(
                bgTint = "#FFAC81",
                imgRes = R.drawable.privacy_policy,
                title  = getString(R.string.privacy_policy)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ─────────────────────────────────────────────────────────────────────
        // FIX #1 — Restore the theme color on the Window BEFORE the layout is
        // inflated.  enableEdgeToEdge() makes the status bar transparent, so on
        // a post-update cold-start the system briefly shows the window's raw
        // background (white) before the fragment toolbar can set its own color.
        // Resolving colorPrimary here and applying it immediately eliminates the
        // white-header flash for every launch, including post-update restarts.
        // ─────────────────────────────────────────────────────────────────────
        restoreThemeColorEarly()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FIX #2 — After enableEdgeToEdge() decorates the window with
        // transparent bars, re-apply the status-bar icon appearance so icons
        // are readable against the themed background (not the white default).
        applyStatusBarAppearance()

        consentController = ConsentController(this)
        consentController.initConsent(deviceId = "D31911EF56FDCB9715391100A2AB57A8", callback = this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupStartGraph()
        checkAutoUpdate()
        handleIntent(intent)
    }

    // -------------------------------------------------------------------------
    // Resolves the theme's colorPrimary and applies it to the Window background
    // BEFORE setContentView, so there is never a white flicker on any launch.
    // -------------------------------------------------------------------------
    private fun restoreThemeColorEarly() {
        try {
            val color = androidx.core.content.ContextCompat.getColor(this, R.color.app_primary_color)
            window.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(color)
            )
        } catch (e: Exception) {
            Log.w("MainActivity", "restoreThemeColorEarly: ${e.message}")
        }
    }

    private fun applyStatusBarAppearance() {
        try {
            val color = androidx.core.content.ContextCompat.getColor(this, R.color.app_primary_color)
            val controller = WindowInsetsControllerCompat(window, binding.root)
            controller.isAppearanceLightStatusBars = isColorLight(color)
        } catch (e: Exception) {
            Log.w("MainActivity", "applyStatusBarAppearance: ${e.message}")
        }
    }
    /**
     * Returns true if [color] is perceptually "light" (luminance > 50 %).
     * Used to decide whether to show dark or light status-bar icons.
     */
    private fun isColorLight(color: Int): Boolean {
        val r = android.graphics.Color.red(color) / 255.0
        val g = android.graphics.Color.green(color) / 255.0
        val b = android.graphics.Color.blue(color) / 255.0
        // Relative luminance (WCAG formula)
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance > 0.5
    }

    fun getBgThemes(): MutableList<com.example.easydiarysatti.utills.BgItem?> = noteBgList
    fun getColorPalette(): List<Int>? = colorPaletteSatti
    fun getDrawerItemList(): List<DrawerItem> = mainDrawerItemList

    private fun setupStartGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
                ?: return

        val navController = navHostFragment.navController
        val inflater = navController.navInflater
        val navGraph = inflater.inflate(R.navigation.mobile_navigation)
        navGraph.setStartDestination(R.id.splashFragment)
        navController.graph = navGraph
    }

    private var isColdStart = true

    override fun onResume() {
        super.onResume()
        val isOnBoardingDone = viewModel.isOnBoardingCompleted()
        if (!isOnBoardingDone) return

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment_activity_main
        ) as? NavHostFragment
        val currentDestinationId = navHostFragment?.navController?.currentDestination?.id

        if (currentDestinationId == R.id.splashFragment) return

        if (sessionManagerRepo.isBypassSecurityLogin()) {
            sessionManagerRepo.bypassSecurityLogin(false)
            return
        }

        if (viewModel.shouldRequireLogin()) {
            showLoginOverlay()
        }
        updateViewModel.checkDownloadedOnResume()
    }

    private fun showLoginOverlay() {
        val overlayContainer = findViewById<FrameLayout>(R.id.loginOverlayContainer)
        overlayContainer.visibility = View.VISIBLE

        val fragment = LoginFragment().apply {
            arguments = Bundle().apply { putBoolean("IS_COLD_START", isColdStart) }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.loginOverlayContainer, fragment)
            .commit()

        isColdStart = false
    }

    fun onLoginFinished() {
        findViewById<FrameLayout>(R.id.loginOverlayContainer).visibility = View.GONE
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
                    is UpdateState.Downloaded -> showRestartSnackBar()
                    else -> {}
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val noteId = intent?.getIntExtra("REMAINDER_UNIQUE_ID", -1) ?: -1
        if (noteId != -1) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
            try {
                navHostFragment?.navController?.navigate(
                    R.id.action_global_remainderFragment,
                    Bundle().apply { putInt("noteId", noteId) }
                )
            } catch (e: Exception) {
                Log.e("Nav", "Global nav failed: ${e.message}")
            }
        }
    }

    private fun showRestartSnackBar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Update ready",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            updateViewModel.completeUpdate()

            // ─────────────────────────────────────────────────────────────────
            // FIX #3 — After completeUpdate() the system kills the process and
            // relaunches it.  Sending a clean launch intent with CLEAR_TOP +
            // NEW_TASK guarantees the Activity is fully recreated (not resumed
            // from the back-stack in a half-initialized state) so the theme
            // color is applied from scratch, preventing the white header.
            // ─────────────────────────────────────────────────────────────────
            packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK   or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
                startActivity(launchIntent)
            }
            finishAffinity()
        }.show()
    }

    override fun onAdsLoad(canRequestAds: Boolean) {
        if (canRequestAds) {
            Log.d("ConsentCheck", "Ads can be requested. Initialize your Ads SDK here.")
            MobileAds.initialize(this) {}
        } else {
            Log.d("ConsentCheck", "Ads cannot be requested.")
        }
    }

    override fun onPolicyStatus(isRequired: Boolean) {
        Log.d("ConsentCheck", "Privacy Options Required: $required")
    }

    override fun onConsentFormDismissed() {
        Log.d("ConsentCheck", "Form dismissed.")
    }
}