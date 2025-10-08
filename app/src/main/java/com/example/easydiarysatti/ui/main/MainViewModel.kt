package com.example.easydiarysatti.ui.main

import androidx.lifecycle.ViewModel
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManagerRepo: SessionManagerRepo
) : ViewModel() {


}