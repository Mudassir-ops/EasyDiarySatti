package com.example.easydiarysatti.ui.createnote

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateNotesViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _notesActionState = Channel<CreateNotesState>(Channel.BUFFERED)
    val notesActionState = _notesActionState.receiveAsFlow()

    private val _noteState = MutableStateFlow<CreateNoteEntity?>(null)
    val noteState: StateFlow<CreateNoteEntity?> = _noteState

    private val _createdNoteId = MutableStateFlow<Long?>(null)
    val createdNoteId: StateFlow<Long?> = _createdNoteId

    private var imagesList: MutableList<String>? = mutableListOf()
    private var tagList: MutableList<String>? = mutableListOf()

    fun sendAction(action: CreateNotesState) {
        viewModelScope.launch {
            Log.e("headerSave", "setClickListeners:$action ")
            _notesActionState.send(action)
        }
    }

    fun mergeAndSave(createNoteEntity: CreateNoteEntity) {
        Log.e("headerSaveSatti", "setClickListeners:$createNoteEntity ")
        viewModelScope.launch {
            createNoteRepository.mergeAndSave(createNoteEntity)
        }
    }

    fun observeNote() {
        viewModelScope.launch {
            createdNoteId.filterNotNull().collect { id ->
                Log.e("observeNote", "observeNote:$id ")
                createNoteRepository.observeNoteById(id).distinctUntilChanged().collect { note ->
                    Log.e("observeNote", "observeNote:$note ")
                    _noteState.value = note
                }
            }
        }
    }

    fun setCurrentNoteId(noteId: Long) {
        viewModelScope.launch {
            _createdNoteId.emit(noteId)
        }
    }

    fun addImages(imagePath: List<String>) {
        imagesList?.addAll(imagePath)
    }

    fun addImage(imagePath: String): List<String> {
        imagesList?.add(imagePath)
        return imagesList?.toList() ?: listOf()
    }


    fun addTags(tags: List<String>) {
        tagList?.addAll(tags)
    }

    fun addTag(tag: String): List<String> {
        tagList?.add(tag)
        return tagList?.toList() ?: listOf()
    }

    fun removeTag(tag: String) {
        tagList?.remove(tag)
    }

}

sealed interface CreateNotesState {
    data object SaveNote : CreateNotesState
    data class ImagePicked(val imageUri: Uri?) : CreateNotesState
    data class AddTag(val tag: String?) : CreateNotesState
    data object DiscardNote : CreateNotesState
    data object BackAction : CreateNotesState
    data class ShowMessage(val msg: String) : CreateNotesState
}