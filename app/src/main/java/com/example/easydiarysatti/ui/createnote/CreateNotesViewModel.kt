package com.example.easydiarysatti.ui.createnote

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
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
    private var tagList = mutableListOf<CustomTagEntity>()

    var imagesCount = 0

    /**
     * Set to true by DraftNotesFragment before opening CreateNotesFragment.
     * Read and immediately cleared by CreateNotesFragment.navigateScreen() to
     * decide whether to go back to DraftNotesFragment or HomeFragment on exit.
     * Using the shared activityViewModel means no intent extras or nav args needed.
     */
    var openedFromDraft: Boolean = false

    fun sendAction(action: CreateNotesState) {
        viewModelScope.launch {
            Log.e("headerSave", "setClickListeners:$action ")
            _notesActionState.send(action)
        }
    }

    /**
     * Normal save — persists note then emits NoteSaved so the Fragment
     * triggers checkInterstitial() → navigateScreen().
     */
    fun mergeAndSave(createNoteEntity: CreateNoteEntity) {
        Log.e("headerSaveSatti", "setClickListeners:$createNoteEntity ")
        viewModelScope.launch {
            createNoteRepository.mergeAndSave(createNoteEntity)
            // ✅ Emit NoteSaved AFTER DB write so the Fragment only calls
            // checkInterstitial() once the save is truly done.
            _notesActionState.send(CreateNotesState.NoteSaved)
        }
    }

    /**
     * Draft save — persists note with isDraft=true then emits Init (NOT NoteSaved).
     *
     * WHY A SEPARATE FUNCTION:
     * mergeAndSave() emits NoteSaved → observeNoteAction() → checkInterstitial()
     * → navigateScreen(). When saving a draft on back-press, proceedWithBackPress()
     * is already queued to handle navigation. Emitting NoteSaved would cause a
     * double-navigation race — second navigateScreen() finds the back stack already
     * popped and crashes back to CreateNote. ❌
     *
     * This function emits Init only, so proceedWithBackPress() is the sole owner
     * of navigation in the draft-save path. ✅
     */
    fun mergeAndSaveAsDraft(createNoteEntity: CreateNoteEntity) {
        Log.e("headerSaveSatti", "mergeAndSaveAsDraft: $createNoteEntity")
        viewModelScope.launch {
            createNoteRepository.mergeAndSave(createNoteEntity.copy(isDraft = true))
            _notesActionState.send(CreateNotesState.Init) // ← NOT NoteSaved
        }
    }
    // Add this to CreateNotesViewModel.kt
    // In CreateNotesViewModel.kt
    fun toggleFavorite(note: CreateNoteEntity) {
        viewModelScope.launch {
            createNoteRepository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }
    fun resetActionState() {
        viewModelScope.launch {
            _notesActionState.send(CreateNotesState.Init)
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

    fun getImageSize(): Int? = imagesList?.size

    fun addImage(imagePath: String): List<String> {
        imagesList?.add(imagePath)
        return imagesList?.toList() ?: listOf()
    }

    fun addTags(tags: List<CustomTagEntity>): List<CustomTagEntity> {
        val validTags = tags.mapNotNull { tagEntity ->
            val cleanedName = tagEntity.tagName.orEmpty().trim()
            if (cleanedName.isNotEmpty()) tagEntity.copy(tagName = cleanedName) else null
        }
        tagList.addAll(validTags)
        tagList = tagList.distinctBy { it.tagName.orEmpty().lowercase() to it.noteId }.toMutableList()
        return tagList.toList()
    }

    fun addTag(tag: String, noteId: Int): List<CustomTagEntity> {
        val cleanTag = tag.trim()
        if (cleanTag.isNotEmpty()) {
            tagList.add(CustomTagEntity(tagName = cleanTag, noteId = noteId))
        }
        tagList = tagList
            .filter { !it.tagName.isNullOrEmpty() }
            .distinctBy { it.tagName.orEmpty().lowercase() to it.noteId }
            .toMutableList()
        return tagList.toList()
    }

    fun removeTag(tag: CustomTagEntity): MutableList<CustomTagEntity> {
        tagList.remove(tag)
        return tagList
    }

    fun removeImage(image: String): List<String>? {
        imagesList = imagesList?.toMutableList().apply { this?.remove(image) }
        return imagesList?.toList()
    }

    fun allTags(): MutableList<CustomTagEntity> = tagList

    fun removeImageDb(noteId: Long, imagesList: List<String>) {
        Log.e("removeImageDb-->", "removeImageDb: $noteId--_$imagesList")
        viewModelScope.launch {
            createNoteRepository.updateImageForNote(noteId = noteId, newImages = imagesList)
        }
    }

    fun clearImages() { imagesList?.clear() }
    fun clearTags()   { tagList.clear() }

    fun updateImagesList(newList: List<String>) {
        imagesList?.clear()
        imagesList?.addAll(newList)
    }

    fun updateTagsForNote(noteId: Long, newTags: List<CustomTagEntity>) {
        viewModelScope.launch {
            createNoteRepository.updateTagsForNote(noteId = noteId, newTags = newTags)
        }
    }
}

sealed interface CreateNotesState {
    data object Init : CreateNotesState
    data object SaveNote : CreateNotesState
    data object NoteSaved : CreateNotesState
    data class ImagePicked(val imageUri: Uri?) : CreateNotesState
    data class AddTag(val tag: String?, val createNoteEntity: CreateNoteEntity?) : CreateNotesState
    data object TagAction : CreateNotesState
    data object DiscardNote : CreateNotesState
    data class FontAction(val font: String) : CreateNotesState
    data class TextAlignment(val alignment: String) : CreateNotesState
    data class HeadingSize(val headingSize: Int) : CreateNotesState
    data class TextColor(val textColor: Int) : CreateNotesState
    data class ChangeBg(val bgImageRes: Int) : CreateNotesState
    data class ChangeBgUri(val bgImageUri: String) : CreateNotesState
    data class ShowMessage(val msg: String) : CreateNotesState
}