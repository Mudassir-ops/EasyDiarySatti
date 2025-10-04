package com.example.easydiarysatti.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.toDateString
import com.example.easydiarysatti.ui.home.HomeNotesState

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

    init {
        observeAllImages()
    }

    fun observeAllImages() {
        createNoteRepository.observeAllNotes()
            .onStart {
                _allImagesState.value = LibraryImagesState.Loading
            }
            .catch { e ->
                _allImagesState.value = LibraryImagesState.Error(e.message ?: "Unknown error")
            }
            .distinctUntilChanged()
            .onEach { notes ->
                if (notes.isNullOrEmpty()) {
                    _allImagesState.value = LibraryImagesState.Error("No images found")
                } else {
                    val groupedItems = notes
                        .filter { it.images?.isNotEmpty() == true }
                        .groupBy { it.creationTime.toDateString() }
                        .flatMap { (date, noteList) ->
                            val items = mutableListOf<LibraryItem>()
                            items.add(LibraryItem.DateItem(date))
                            noteList.forEach { note ->
                                note.images?.forEach { imagePath ->
                                    items.add(
                                        LibraryItem.ImagesItem(
                                            date = date,
                                            imagePaths = imagePath,
                                            noteTitle = note.title ?: "no title"
                                        )
                                    )
                                }
                            }
                            items
                        }

                    _allImagesState.value = LibraryImagesState.Success(groupedItems)
                }
            }
            .launchIn(viewModelScope)
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
