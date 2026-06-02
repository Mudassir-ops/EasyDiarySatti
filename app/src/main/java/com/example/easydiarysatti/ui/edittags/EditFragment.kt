package com.example.easydiarysatti.ui.edittags

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.easydiarysatti.R
import com.example.easydiarysatti.ads.natives.presentation.enums.NativeAdKey
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeLargeView
import com.example.easydiarysatti.ads.natives.presentation.ui.AdNativeSmallView
import com.example.easydiarysatti.ads.natives.presentation.viewModels.ViewModelNative
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.databinding.FragmentEditBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.loadBackground
import com.example.easydiarysatti.ui.createnote.CreateNotesViewModel
import com.example.easydiarysatti.utills.getCurrentThemeColor
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class EditFragment : Fragment(R.layout.fragment_edit) {
    private var noteEntity: CreateNoteEntity? = null
    private val nativeViewModel: ViewModelNative by viewModels()
    private val binding by viewBinding(FragmentEditBinding::bind)
    private val viewModel: CreateNotesViewModel by activityViewModels()

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            // 1. THIS REMOVES THE EXTRA SYSTEM HEADER
            (activity as? AppCompatActivity)?.supportActionBar?.hide()
            val themeColor=getCurrentThemeColor(sessionManagerRepo)
//            btnNext.backgroundTintList = ColorStateList.valueOf(themeColor)
            btnNext.setOnClickListener {
                if (etTags.text.toString().isEmpty()) {
                    findNavController().navigateUp()
                    return@setOnClickListener
                }
//                viewModel.sendAction(
//                    action = CreateNotesState.AddTag(
//                        tag = etTags.text.toString(),
//                        createNoteEntity = viewModel.noteState.value
//                    )
//                )
                findNavController().navigateUp()
            }
            ivMenu.setOnClickListener {
                findNavController().navigateUp()
            }
        }
        setupBgTheme()
//        setupNativeAd()
    }
//    private fun setupNativeAd() {
//        // 1. Observe the LiveData
//        nativeViewModel.adViewLiveData.observe(viewLifecycleOwner) { nativeAd ->
//            if (nativeAd != null) {
//                val adSmallView = AdNativeSmallView(requireContext())
//                binding?.flAdplaceholder?.apply {
//                    removeAllViews()
//                    addView(adSmallView)
//                    adSmallView.setNativeAd(nativeAd)
//                }
//            }
//        }
//
//        // 2. Request the ad (using the ON_BOARDING or appropriate key)
////        nativeViewModel.loadNativeAd(NativeAdKey.PERMISSION)
//    }
    private fun setupBgTheme() {
        val bgResource = sessionManagerRepo.getBgTheme()
        val finalResource = if (bgResource != 0) bgResource else R.drawable.theme_1

        binding?.parentView?.let { view ->
            Glide.with(this)
                .load(finalResource)
                // Fix: Force Glide to bypass memory cache to ensure theme change is reflected
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(object : CustomViewTarget<View, Drawable>(view) {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        view.background = resource
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                        view.background = null
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        view.setBackgroundResource(R.drawable.theme_1)
                    }
                })
        }
    }

}