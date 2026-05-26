package com.example.easydiarysatti.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.banner.presentation.enums.BannerAdKey
import com.example.easydiarysatti.ads.banner.presentation.viewModels.ViewModelBanner
import com.example.easydiarysatti.ads.manager.InternetManager
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.utils.addCleanView
import com.example.easydiarysatti.databinding.FragmentPermissionBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.showPermissionDialog
import com.example.easydiarysatti.viewBinding
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class PermissionFragment : Fragment(R.layout.fragment_permission) {
    private val binding by viewBinding(FragmentPermissionBinding::bind)
    private val bannerViewModel by activityViewModels<ViewModelBanner>()

    @Inject lateinit var sharedPref: SharedPreferenceUtils
    @Inject lateinit var internetManager: InternetManager
    private var cameraDeniedCount = 0
    private var galleryDeniedCount = 0
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
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

    private fun setupBannerObserver() {
        if (sharedPref.isAppPurchased||!internetManager.isInternetConnected || !sharedPref.getAdShowStatus(BannerAdKey.PERMISSION.value)) {
            binding?.bannerShimmerContainer?.visibility = View.GONE
            binding?.bannerContainer?.visibility = View.GONE
            return
        }
        bannerViewModel.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            val preloadedAd = adMap[BannerAdKey.PERMISSION]
            if (preloadedAd != null) {
                binding?.bannerShimmerContainer?.visibility = View.VISIBLE
                binding?.bannerShimmerContainer?.startShimmer()

                if (isAdded && binding != null) {
                    binding?.bannerShimmerContainer?.stopShimmer()
                    binding?.bannerShimmerContainer?.setShimmer(null)

                    binding?.bannerContainer?.let { container ->
                        container.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        container.addCleanView(preloadedAd)
                        container.visibility = View.VISIBLE
                    }
                }
            } else {
                binding?.bannerShimmerContainer?.visibility = View.GONE
                binding?.bannerContainer?.visibility = View.GONE
            }
        }
    }

    /**
     * Waterfall navigation driven by the onboarding flow JSON.
     *
     * We ask the JSON "what comes after permission?" and navigate there.
     * The JSON already encodes which screens exist in the active case, so we
     * never need to manually check individual boolean flags here.
     *
     * Example flows:
     *   case_full           → permission → name → pin → theme → home
     *   case_no_name        → permission → pin  → theme → home
     *   case_no_name_no_pin → permission → theme → home
     */
    fun moveToNextScreen() {
        // Mark permission as done so onboarding never routes here again,
        // even if the flow JSON still lists it for new installs.
        sharedPref.isPermissionDone = true

        val nextScreen = sharedPref.nextScreenAfter(SharedPreferenceUtils.SCREEN_PERMISSION)
        Log.d("PermissionFragment", "Next screen after permission: $nextScreen")

        when (nextScreen) {
            SharedPreferenceUtils.SCREEN_NAME -> {
                preLoadNextAd(BannerAdKey.START_WRITING)
                findNavController().navigate(R.id.action_permissionFragment_to_nameFragment)
            }
            SharedPreferenceUtils.SCREEN_PIN -> {
                preLoadNextAd(BannerAdKey.PIN_SETUP)
                findNavController().navigate(R.id.action_permissionFragment_to_pinSetupFragment)
            }
            SharedPreferenceUtils.SCREEN_THEME -> {
                preLoadNextAd(BannerAdKey.THEME_SELECTION)
                findNavController().navigate(R.id.action_permissionFragment_to_themeFragment)
            }
            SharedPreferenceUtils.SCREEN_HOME, null -> {
                // Either home is next, or permission was the last screen before home
                sharedPref.isFirstTimeUser = false
                findNavController().navigate(R.id.action_permissionFragment_to_mainFragment)
            }
        }
    }

    private fun preLoadNextAd(adKey: BannerAdKey) {
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
            if (cameraGranted && galleryGranted) {
                moveToNextScreen()
            }
        }
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