package com.example.easydiarysatti.ui.previewnote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _allNotesPreviewState = MutableStateFlow<CreateNoteEntity?>(null)
    val allNotesPreviewState: StateFlow<CreateNoteEntity?> = _allNotesPreviewState

    fun getNoteById(noteId: Long) {
        viewModelScope.launch {
            _allNotesPreviewState.emit(createNoteRepository.getNoteById(id = noteId))
        }
    }

}