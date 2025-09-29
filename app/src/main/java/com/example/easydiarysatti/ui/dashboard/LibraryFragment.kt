package com.example.easydiarysatti.ui.dashboard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLibraryBinding
import com.example.easydiarysatti.toDateString
import com.example.easydiarysatti.ui.dashboard.MultiViewAdapter.Companion.TYPE_DATE
import com.example.easydiarysatti.ui.dashboard.MultiViewAdapter.Companion.TYPE_IMAGE
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val binding by viewBinding(FragmentLibraryBinding::bind)
    private val viewModel by viewModels<LibraryViewModel>()
    private val items: List<LibraryItem> by lazy {
        listOf(
            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),
            LibraryItem.DateItem("1759174758".toLong().toDateString()),

            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3),

            LibraryItem.DateItem("1759174758".toLong().toDateString()),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_1),
            LibraryItem.ImagesItem("1759174758".toLong().toDateString(), R.drawable.theme_3)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLibraryImages()
    }

    private fun setupLibraryImages() {
        val adapter = MultiViewAdapter(items) { imagePath, date ->
            val bundle = Bundle().apply {
                putString("image_path", imagePath)
                putString("date", date)
            }
//            findNavController().navigate(R.id.imageViewFragment, bundle)
//            Log.d("ImageViewFragment", "Image send: $imagePath, date:$date")
        }

        val layoutManager = GridLayoutManager(context ?: return, 2)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    TYPE_DATE -> 2
                    TYPE_IMAGE -> 1
                    else -> 1
                }
            }
        }

        binding?.libraryRecyclerView?.apply {
            this.layoutManager = layoutManager
            this.adapter = adapter
        }
    }

}