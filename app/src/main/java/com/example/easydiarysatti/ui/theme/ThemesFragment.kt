package com.example.easydiarysatti.ui.theme

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentNameBinding
import com.example.easydiarysatti.databinding.FragmentNameBinding.bind
import com.example.easydiarysatti.databinding.FragmentThemesBinding
import com.example.easydiarysatti.ui.name.NameViewModel
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class ThemesFragment : Fragment(R.layout.fragment_themes) {
    private val viewModel by viewModels<NameViewModel>()
    private val binding by viewBinding(FragmentThemesBinding::bind)

    private var themeAdapter: ThemeAdapter? = null
    private val themesList: List<Int> by lazy {
        listOf(
            R.drawable.theme_2,
            R.drawable.theme_1,
            R.drawable.theme_3,
            R.drawable.theme_4,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeAdapter = ThemeAdapter(themes = themesList, onThemeClick = {})
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            themeViewPager.adapter = themeAdapter
            themeViewPager.offscreenPageLimit = 4
            themeViewPager.setPageTransformer { page, position ->
                page.scaleY = 0.85f + (1 - abs(position)) * 0.15f
            }
            themeViewPager.setCurrentItem(1, false)
        }
    }
}