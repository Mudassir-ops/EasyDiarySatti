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

    private var createNoteEntity: CreateNoteEntity? = CreateNoteEntity()

    private var imagesList: MutableList<String>? = mutableListOf()

    fun sendAction(action: CreateNotesState) {
        viewModelScope.launch {
            Log.e("headerSave", "setClickListeners:$action ")
            _notesActionState.send(action)
        }
    }


    fun mergeAndSave(createNoteEntity: CreateNoteEntity) {
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
                    createNoteEntity = note
                }
            }
        }
    }

    fun updateTitle(title: String) {
        createNoteEntity = createNoteEntity?.copy(title = title)
    }

    fun updateDescription(description: String) {
        createNoteEntity = createNoteEntity?.copy(description = description)
    }

    fun updateFeelingTitle(feelingTitle: String) {
        createNoteEntity = createNoteEntity?.copy(feelingTitle = feelingTitle)
    }

    fun updateTextColor(color: String) {
        createNoteEntity = createNoteEntity?.copy(textColor = color)
    }

    fun updateFeelingEmoji(emojiRes: Int) {
        createNoteEntity = createNoteEntity?.copy(feelingEmojiRes = emojiRes)
    }

    fun updateBackground(bgRes: Int) {
        createNoteEntity = createNoteEntity?.copy(backgroundRes = bgRes)
    }


    fun updateTextSizeH1(size: Float) {
        createNoteEntity = createNoteEntity?.copy(textSizeH1 = size)
    }

    fun updateTextSizeH2(size: Float) {
        createNoteEntity = createNoteEntity?.copy(textSizeH2 = size)
    }

    fun updateTextSizeH3(size: Float) {
        createNoteEntity = createNoteEntity?.copy(textSizeH3 = size)
    }

    fun updateFont(font: String) {
        createNoteEntity = createNoteEntity?.copy(textFont = font)
    }

    fun updateAlignment(alignment: String) {
        createNoteEntity = createNoteEntity?.copy(textAlignment = alignment)
    }

    fun updateText(text: String) {
        createNoteEntity = createNoteEntity?.copy(text = text)
    }

    fun updateCreationTime(time: Long) {
        createNoteEntity = createNoteEntity?.copy(creationTime = time)
    }

    // --- LIST UPDATES ---
    fun addTag(tag: String) {
        val updatedTags = (createNoteEntity?.tags ?: emptyList()) + tag
        createNoteEntity = createNoteEntity?.copy(tags = updatedTags.distinct())
    }

    fun removeTag(tag: String) {
        val updatedTags = (createNoteEntity?.tags ?: emptyList()) - tag
        createNoteEntity = createNoteEntity?.copy(tags = updatedTags)
    }

    fun addImage(imagePath: String): List<String> {
        return (imagesList ?: emptyList()) + imagePath
    }

    fun removeImage(imagePath: String) {
        val updatedImages = (createNoteEntity?.images ?: emptyList()) - imagePath
        createNoteEntity = createNoteEntity?.copy(images = updatedImages)
    }

    suspend fun getOldImages() {


    }

    // --- CLEAR ON DESTROY ---
    override fun onCleared() {
        super.onCleared()
        createNoteEntity = null
    }

}

sealed interface CreateNotesState {
    data object SaveNote : CreateNotesState
    data class ImagePicked(val imageUri: Uri?) : CreateNotesState
    data object DiscardNote : CreateNotesState
    data object BackAction : CreateNotesState
    data class ShowMessage(val msg: String) : CreateNotesState
}