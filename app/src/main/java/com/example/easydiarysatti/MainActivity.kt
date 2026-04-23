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
import androidx.core.view.WindowInsetsCompat
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
    // 1. Declare the controller
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
            null,                                                              // position 0 → upload button
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
            "#334155".toColorInt(), // black-ish
            "#64748B".toColorInt(), // dark gray
            "#8478BF".toColorInt(), // light gray
            "#0F2A45".toColorInt(), // pink-ish
            "#0F172A".toColorInt(), // greenish
            "#4C0821".toColorInt()  // purple
        )
    }

    /**
     * Drawer item list — positions must match the when(it) block in MainFragment.drawerItemAdapter.
     *   0 → Edit Tags
     *   1 → Draft          ← NEW
     *   2 → Color Theme    (was 1)
     *   3 → Reminders      (was 2)
     *   4 → Diary Lock     (was 3)
     *   5 → Language       (was 4)
     *   6 → Privacy Policy (was 5)
     *
     * Add to strings.xml:  <string name="draft">Draft</string>
     */
    private val mainDrawerItemList: List<DrawerItem> by lazy {
        listOf(
            DrawerItem(                        // 0 — Edit Tags
                bgTint = "#FFAC81",
                imgRes = R.drawable.pencil_icon,
                title  = getString(R.string.edit_tags2)
            ),
            DrawerItem(                        // 1 — Favorites (NEW)
                bgTint = "#E8BA00",            // pink/red to match heart theme
                imgRes = R.drawable.ic_file_favourite,
                title  = getString(R.string.favorites)
            ),
            DrawerItem(                        // 1 — Draft (NEW)
                bgTint = "#42ABD0",
                imgRes = R.drawable.ic_draft_document1,
                title  = getString(R.string.draft)
            ),
            DrawerItem(                        // 2 — Color Theme (was 1)
                bgTint = "#5EE3A9",
                imgRes = R.drawable.paint_icon,
                title  = getString(R.string.color_theme)
            ),
            DrawerItem(                        // 3 — Reminders (was 2)
                bgTint = "#FFDE8B",
                imgRes = R.drawable.bell_drawer,
                title  = getString(R.string.remainders)
            ),
            DrawerItem(                        // 4 — Diary Lock (was 3)
                bgTint = "#FF8D95",
                imgRes = R.drawable.lock,
                title  = getString(R.string.dairy_lock)
            ),
            DrawerItem(                        // 5 — Language (was 4)
                bgTint = "#A29DFB",
                imgRes = R.drawable.language_icon,
                title  = getString(R.string.langauge)
            ),
            DrawerItem(                        // 6 — Privacy Policy (was 5)
                bgTint = "#FFAC81",
                imgRes = R.drawable.privacy_policy,
                title  = getString(R.string.privacy_policy)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        if (viewModel.isOnBoardingCompleted()) {
            navGraph.setStartDestination(R.id.splashFragment)
        } else {
            navGraph.setStartDestination(R.id.splashFragment)
        }
        navController.graph = navGraph

    }

    private var isColdStart = true // Will be true when app is first opened

    override fun onResume() {
        super.onResume()
        val isOnBoardingDone = viewModel.isOnBoardingCompleted()
        if (!isOnBoardingDone) return

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
        val currentDestinationId = navHostFragment?.navController?.currentDestination?.id

        // 1. DO NOT show login if we are on Splash.
        // This allows Splash to show and Ads to initialize properly.
        if (currentDestinationId == R.id.splashFragment) {
            return
        }

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

        // Pass the Cold Start flag to the Fragment
        val fragment = LoginFragment().apply {
            arguments = Bundle().apply { putBoolean("IS_COLD_START", isColdStart) }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.loginOverlayContainer, fragment)
            .commit()

        // After it shows once, any other trigger in this session is a resume
        isColdStart = false
    }

    // Function for LoginFragment to call
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
                    is UpdateState.Downloaded -> {
                        showRestartSnackBar()
                    }

                    else -> {}
                }
            }
        }
    }
    // Inside MainActivity.kt

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    //    private fun handleIntent(intent: Intent?) {
//        val noteId = intent?.getIntExtra(REMAINDER_UNIQUE_ID, -1) ?: -1
//        if (noteId != -1) {
//            // Use your safeNav extension to go to the note screen
//            val navHostFragment = supportFragmentManager
//                .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment
//            navHostFragment?.navController?.safeNav(
//                currentDestId = R.id.mainFragment, // Or current destination
//                actionId = R.id.remainderFragment,
//                bundle = Bundle().apply { putInt("noteId", noteId) }
//            )
//        }
//    }
// Inside MainActivity.kt
    private fun handleIntent(intent: Intent?) {
        val noteId = intent?.getIntExtra("REMAINDER_UNIQUE_ID", -1) ?: -1
        if (noteId != -1) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_activity_main) as? NavHostFragment

            // Use global action instead of fragment ID to avoid crashes
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
        )
            .setAction("Restart") {
                updateViewModel.completeUpdate()
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
        // This tells you if the "Privacy Options" entry point needs to be visible in settings
        Log.d("ConsentCheck", "Privacy Options Required: $required")
    }
    override fun onConsentFormDismissed() {
        Log.d("ConsentCheck", "Form dismissed.")
    }
}