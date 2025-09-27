package com.example.easydiarysatti.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.easydiarysatti.ON_GOING_DATA_MODEL
import com.example.easydiarysatti.ui.onboarding.ongoingitem.OnGoingItemFragment
import com.example.easydiarysatti.ui.uimodels.OnGoingScreenUiModel

class OnGoingPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val onGoingPagesList: Array<OnGoingScreenUiModel>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = onGoingPagesList.size

    override fun createFragment(position: Int): Fragment {
        return OnGoingItemFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ON_GOING_DATA_MODEL, onGoingPagesList[position])
            }
        }
    }

}