package com.example.easydiarysatti.ui.createnote

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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


    private var imagesList: MutableList<String>? = mutableListOf()
    private var tagList = mutableListOf<String>()

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

    fun setupNoteEntity(createNoteEntity: CreateNoteEntity?) {
        viewModelScope.launch {
            _noteState.value = createNoteEntity
        }
    }

    fun addImages(imagePath: List<String>) {
        imagesList?.addAll(imagePath)
    }

    fun addImage(imagePath: String): List<String> {
        imagesList?.add(imagePath)
        return imagesList?.toList() ?: listOf()
    }


    fun addTags(tags: List<String>): List<String> {
        val validTags = tags.mapNotNull { it.trim().takeIf { tag -> tag.isNotEmpty() } }
        tagList.addAll(validTags)
        tagList = tagList.distinct().toMutableList()
        return tagList.toList()
    }

    fun addTag(tag: String): List<String> {
        val cleanTag = tag.trim()
        if (cleanTag.isNotEmpty()) {
            tagList.add(cleanTag)
        }
        tagList = tagList.filter { it.isNotEmpty() }.distinct().toMutableList()
        return tagList
    }


    fun removeTag(tag: String) {
        tagList.remove(tag)
    }

    fun clearImages() {
        imagesList?.clear()
    }

    fun clearTags() {
        tagList.clear()
    }

}

sealed interface CreateNotesState {
    data object Init : CreateNotesState
    data object SaveNote : CreateNotesState
    data class ImagePicked(val imageUri: Uri?) : CreateNotesState
    data class AddTag(val tag: String?, val createNoteEntity: CreateNoteEntity?) : CreateNotesState
    data object TagAction : CreateNotesState
    data object DiscardNote : CreateNotesState
    data class FontAction(val font: String) : CreateNotesState
    data class TextAlignment(val alignment: String) : CreateNotesState
    data class HeadingSize(val headingSize: Int) : CreateNotesState
    data class TextColor(val textColor: Int) : CreateNotesState
    data class ChangeBg(val bgImageRes: Int) : CreateNotesState
    data class ShowMessage(val msg: String) : CreateNotesState
}