package com.example.easydiarysatti.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentPermissionBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showPermissionDialog
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val binding by viewBinding(FragmentPermissionBinding::bind)

    // Banner ViewModel to pre-load the next ad
    private val bannerViewModel by activityViewModels<ViewModelBanner>()

    @Inject lateinit var sharedPref: SharedPreferenceUtils

    private var cameraDeniedCount = 0
    private var galleryDeniedCount = 0
    lateinit var mFirebaseAnalytics : FirebaseAnalytics
    private lateinit var requestCameraPermission: ActivityResultLauncher<String>
    private lateinit var requestGalleryPermission: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        requestCameraPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    binding?.icSwitchCamera?.isChecked = true
                    cameraDeniedCount = 0
                } else {
                    binding?.icSwitchCamera?.isChecked = false
                    cameraDeniedCount++
                    if (cameraDeniedCount >= 2) {
                        showPermissionDialog(context ?: return@registerForActivityResult, this)
                    }
                }
                checkAllPermissions()
            }

        requestGalleryPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val allGranted = results.all { it.value }
                if (allGranted) {
                    binding?.icSwitchGallery?.isChecked = true
                    galleryDeniedCount = 0
                } else {
                    binding?.icSwitchGallery?.isChecked = false
                    galleryDeniedCount++
                    if (galleryDeniedCount >= 2) {
                        showPermissionDialog(context ?: return@registerForActivityResult, this)
                    }
                }
                checkAllPermissions()
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
        checkAllPermissions()
          setupBannerObserver()


    }
    // 1. Define this variable at the top of your Fragment class (not inside the function)
//    private var isAdProcessStarted = false

    private fun setupBannerObserver() {
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            // 2. SAFETY LOCK: If we already started the timer for this screen session, STOP here.
//            if (isAdProcessStarted) return@observe

            // Specifically grab the ad preloaded for this screen
            val preloadedAd = adMap[BannerAdKey.PERMISSION]

            if (preloadedAd != null) {
                // 3. ACTIVATE LOCK: Ensure this block only runs once
//                isAdProcessStarted = true

                // Make sure the shimmer container is visible and playing
                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()


                    // Check if fragment is still alive to avoid crashes
                    if (isAdded && binding != null) {

                        // 5. TURN OFF SHIMMER: Stop the animation
                        binding?.bannerShimmerContainer?.stopShimmer()
                        binding?.bannerShimmerContainer?.setShimmer(null)

                        binding?.bannerContainer?.let { container ->
                            // Remove gray color and show the real ad
                            container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            container.addCleanView(preloadedAd)
                            container.visibility = View.VISIBLE
                        }
                        Log.d("AdDebug", "Shimmer off, PERMISSION Ad visible (One-time load)")
                    }

            } else {
                // If the ad isn't ready in the map, hide the container
                binding?.bannerShimmerContainer?.visibility = View.GONE
                Log.d("AdDebug", "PERMISSION ad not found in map yet...")
            }
        }
    }

    fun moveToNextScreen() {
        val showWriting = sharedPref.isNameWritingEnabled
        val showPin = sharedPref.isPinSetupEnabled
        val showTheme = sharedPref.isThemeSelectionEnabled
        Log.d("WaterfallDebug", "isNameWritingEnabled: $showWriting")
        // Define NavOptions to POP the PermissionFragment so it's not in the history
        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.permissionFragment, true)
            .build()
        when {
            // Path 1: Start Writing is Enabled
            showWriting -> {
                preLoadNextAd(BannerAdKey.START_WRITING)
                findNavController().navigate(
                    R.id.action_permissionFragment_to_nameFragment,
                    null,
                    navOptions // Apply the fix here
                )
            }

            // Path 2: Skip Writing -> Check Pin Setup
            showPin -> {
                preLoadNextAd(BannerAdKey.PIN_SETUP)
                findNavController().navigate(
                    R.id.action_permissionFragment_to_pinSetupFragment,
                    null,
                    navOptions // Apply the fix here
                )
            }

            // Path 3: Skip Writing & Pin -> Check Theme Selection
            showTheme -> {
                preLoadNextAd(BannerAdKey.THEME_SELECTION)
                findNavController().navigate(
                    R.id.action_permissionFragment_to_themeFragment,
                    null,
                    navOptions // Apply the fix here
                )
            }

            // Path 4: ALL REMAINING SETUP SKIPPED -> Go Home
            else -> {
                sharedPref.isFirstTimeUser = false
                findNavController().navigate(
                    R.id.action_permissionFragment_to_mainFragment,
                    null,
                    navOptions // Apply the fix here
                )
            }
        }
    }

    private fun preLoadNextAd(adKey: BannerAdKey) {
        Log.d("AdsInformation", "Permission screen calling ad for next: ${adKey.value}")
        bannerViewModel.loadBannerAd(
            com.google.android.gms.ads.AdView(requireContext()),
            adKey,
            requireContext()
        )
    }

    private fun checkAllPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        val galleryGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            val read = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            read && write
        }

        binding?.apply {
            icSwitchCamera.isChecked = cameraGranted
            icSwitchGallery.isChecked = galleryGranted

            // AUTOMATIC FORWARD LOGIC
            if (cameraGranted && galleryGranted) {
                moveToNextScreen()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset so the next time the user opens this fragment, the shimmer runs again
//        isAdProcessStarted = false
    }
    private fun clickListener() {
        binding?.apply {
            icSwitchCamera.setOnClickListener {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
            icSwitchGallery.setOnClickListener {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                requestGalleryPermission.launch(permissions.toTypedArray())
            }

            btnSkip.setOnClickListener {
                val eventParams = Bundle().apply { putString("action_type", "skip_clicked") }
                mFirebaseAnalytics.logEvent("On_Boarding_Permissions_Skipped", eventParams)
                moveToNextScreen()
            }
        }
    }
}