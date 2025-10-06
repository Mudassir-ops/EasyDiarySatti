package com.example.easydiarysatti.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLibraryBinding
import com.example.easydiarysatti.ui.dashboard.MultiViewAdapter.Companion.TYPE_DATE
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val binding by viewBinding(FragmentLibraryBinding::bind)
    private val viewModel by viewModels<LibraryViewModel>()

    private var adapter: MultiViewAdapter? = null
    private var layoutManager: GridLayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeAllImages()
    }

    private fun setupRecyclerView() {
        adapter = MultiViewAdapter { imagePath, date ->

        }
        layoutManager = GridLayoutManager(context ?: return, 2)
        binding?.libraryRecyclerView?.adapter = adapter
        binding?.libraryRecyclerView?.layoutManager = layoutManager
        binding?.libraryRecyclerView?.setHasFixedSize(true)
        layoutManager?.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val type = adapter?.getItemViewType(position)
                return if (type == TYPE_DATE) layoutManager?.spanCount ?: 0 else 1
            }
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

