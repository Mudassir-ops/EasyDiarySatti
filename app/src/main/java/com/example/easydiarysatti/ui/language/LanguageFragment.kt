package com.example.easydiarysatti.ui.language

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.easydiarysatti.R
import com.example.easydiarysatti.databinding.FragmentLanguageBinding
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import com.example.easydiarysatti.setSelectedBg
import com.example.easydiarysatti.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LanguageFragment : Fragment(R.layout.fragment_language) {
    private val viewModel by viewModels<LanguageViewModel>()
    private val binding by viewBinding(FragmentLanguageBinding::bind)

    @Inject
    lateinit var sessionManagerRepo: SessionManagerRepo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        clickListener()
    }

    private fun clickListener() {
        binding?.apply {
            btnNext.setOnClickListener { moveToNextScreen() }
            val languageItems =
                listOf(
                    clEnglishUk,
                    clEnglishUs,
                    clArabic,
                    clPortuguese,
                    clUrdu,
                    clTurkish,
                    clSpanish
                )
            languageItems.forEach { item ->
                item.setOnClickListener { clickedView ->
                    languageItems.forEach { it.setSelectedBg(it == clickedView) }
                }
            }
        }
    }

    fun moveToNextScreen() {
        sessionManagerRepo.setLanguageSettled(languageSettledIn = true)
        /* val alreadyGranted = validateStoragePermission()
         if (alreadyGranted) {
             findNavController().safeNav(
                 currentDestId = R.id.languageFragment,
                 actionId = R.id.action_languageFragment_to_onBoardingFragment
             )
         } else {
             findNavController().safeNav(
                 currentDestId = R.id.languageFragment,
                 actionId = R.id.action_languageFragment_to_permissionFragment
             )
         }*/
    }
}