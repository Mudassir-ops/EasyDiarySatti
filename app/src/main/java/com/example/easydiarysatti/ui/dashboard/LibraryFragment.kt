package com.example.easydiarysatti.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.easydiarysatti.FROM_SCREEN
import com.example.easydiarysatti.NOTE_ID
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.manager.SharedPreferenceUtils
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.databinding.FragmentLibraryBinding
import com.example.easydiarysatti.safeNav
import com.example.easydiarysatti.ui.dashboard.MultiViewAdapter.Companion.TYPE_DATE
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val binding by viewBinding(FragmentLibraryBinding::bind)
    private val viewModel by viewModels<LibraryViewModel>()
    private val viewModelNative: ViewModelNative by activityViewModels()
    private var adapter: MultiViewAdapter? = null
    private var layoutManager: GridLayoutManager? = null
    @Inject
    lateinit var sharedPreferenceUtils: SharedPreferenceUtils
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Default is 2 as per your spreadsheet
        // Fetch row from Remote Config (bridge via SharedPrefs)
        val adRow = sharedPreferenceUtils.libraryNativeAdAfterItems.toIntOrNull() ?: 2

        // Now start the observation with the correct row
        viewModel.observeAllImages(adRow)
        setupRecyclerView()
        observeAllImages()

        initAdObserver()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        adAlreadyPassedToAdapter = false
    }
    private var adAlreadyPassedToAdapter = false
    private fun initAdObserver() {
        viewModelNative.adMapLiveData.observe(viewLifecycleOwner) { adMap ->
            val nativeAd = adMap[NativeAdKey.LIBRARY]
            if (nativeAd != null) {
                // Pass the loaded ad to your adapter
                adAlreadyPassedToAdapter = true
                adapter?.setNativeAd(nativeAd)
            }
        }
    }
    private fun setupRecyclerView() {
        adapter = MultiViewAdapter { imagePaths, date, noteId ->
            findNavController().safeNav(
                currentDestId = R.id.libraryFragment,
                actionId = R.id.action_libraryFragment_to_previewFragment,
                bundle = Bundle().apply {
                    putLong(NOTE_ID, noteId)
                    putBoolean(FROM_SCREEN, false)
                }
            )
        }

        // Initialize ONLY ONE layout manager
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter?.getItemViewType(position)) {
                    MultiViewAdapter.TYPE_DATE -> 2 // Full Width
                    MultiViewAdapter.TYPE_AD -> 2   // Full Width
                    else -> 1                      // 1 Column
                }
            }
        }

        binding?.libraryRecyclerView?.apply {
            this.layoutManager = gridLayoutManager
            this.adapter = this@LibraryFragment.adapter
            setHasFixedSize(true)
        }
    }



    private fun observeAllImages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allImagesState.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { state ->
                    when (state) {
                        is LibraryImagesState.Success -> {
                            if (state.libraryItems.isEmpty()) {
                                binding?.libraryRecyclerView?.visibility = View.GONE
                                binding?.tvNoData?.visibility = View.VISIBLE
                                return@collect
                            }
                            binding?.libraryRecyclerView?.visibility = View.VISIBLE
                            binding?.tvNoData?.visibility = View.GONE
                            adapter?.submitList(state.libraryItems)
                        }

                        is LibraryImagesState.Error -> {
                            binding?.libraryRecyclerView?.visibility = View.GONE
                            binding?.tvNoData?.visibility = View.VISIBLE
                            Log.e("LibraryImagesState", "Error loading images")
                        }

                        else -> Unit
                    }
                }
        }
    }
}

