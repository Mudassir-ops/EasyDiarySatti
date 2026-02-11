package com.example.easydiarysatti.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.toDateString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _allImagesState = MutableStateFlow<LibraryImagesState>(LibraryImagesState.Loading)
    val allImagesState: StateFlow<LibraryImagesState> = _allImagesState
//
//    init {
//        observeAllImages()
//    }

    // LibraryViewModel.kt
    fun observeAllImages(targetRow: Int) {
        createNoteRepository.observeAllImages()
            .onStart { _allImagesState.value = LibraryImagesState.Loading }
            .distinctUntilChanged()
            .onEach { notes ->
                if (notes.isNullOrEmpty()) {
                    _allImagesState.value = LibraryImagesState.Error("No data")
                } else {
                    val baseItems = mutableListOf<LibraryItem>()
                    val groupedMap = notes.filter { it.images?.isNotEmpty() == true }
                        .groupBy { it.creationTime.toDateString() }

                    groupedMap.forEach { (date, noteList) ->
                        baseItems.add(LibraryItem.DateItem(date))
                        noteList.forEach { note ->
                            baseItems.add(LibraryItem.ImagesItem(date, note.images!!, note.title ?: "", note.noteId))
                        }
                    }

                    val finalItemsList = mutableListOf<LibraryItem>()

                    // ADJUSTMENT: We want to skip (targetRow) full rows of content.
                    // If targetRow is 2, we want 2 full rows (4 slots) to pass.
                    val targetSlotCount = targetRow * 2

                    var currentSlotCount = 0
                    var adInserted = false

                    for (item in baseItems) {
                        val itemSpan = if (item is LibraryItem.DateItem) 2 else 1

                        finalItemsList.add(item)
                        currentSlotCount += itemSpan
                        if (!adInserted && currentSlotCount == targetSlotCount) {
                            finalItemsList.add(LibraryItem.AdItem)
                            adInserted = true
                        }

                    }

                    _allImagesState.value = LibraryImagesState.Success(finalItemsList)
                }
            }.launchIn(viewModelScope)
    }

    fun String?.toFormattedDate(): String {
        if (this.isNullOrEmpty()) return "Unknown Date"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(this)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "Unknown Date"
        }
    }
}

open class LibraryImagesState(
    open val libraryItems: List<LibraryItem> = emptyList()
) {
    object Loading : LibraryImagesState()
    data class Success(override val libraryItems: List<LibraryItem>) : LibraryImagesState()
    data class Error(val message: String) : LibraryImagesState()
}
