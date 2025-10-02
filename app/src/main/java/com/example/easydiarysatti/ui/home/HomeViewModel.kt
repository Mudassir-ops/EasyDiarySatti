package com.example.easydiarysatti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import com.example.easydiarysatti.data.repo.impl.EasyDiaryLocalDataSourceImpl
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    fun createEmptyNote() {
        viewModelScope.launch {
            createNoteRepository.createEmptyNote()
        }
    }

}