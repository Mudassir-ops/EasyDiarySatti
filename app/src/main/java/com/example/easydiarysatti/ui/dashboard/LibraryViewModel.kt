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

    init {
        observeAllImages()
    }

    fun observeAllImages() {
        createNoteRepository.observeAllImages()
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
                    // 1. Create one single master list
                    val finalItemsList = mutableListOf<LibraryItem>()

                    // 2. Group and process the data
                    val groupedMap = notes
                        .filter { it.images?.isNotEmpty() == true }
                        .groupBy { it.creationTime.toDateString() }

                    groupedMap.forEach { (date, noteList) ->
                        // Add Date Header
                        finalItemsList.add(LibraryItem.DateItem(date))

                        // Add Image Items
                        noteList.forEach { note ->
                            finalItemsList.add(
                                LibraryItem.ImagesItem(
                                    date = date,
                                    imagePaths = note.images ?: emptyList(),
                                    noteTitle = note.title ?: "no title",
                                    noteId = note.noteId
                                )
                            )
                        }
                    }

                    // 3. Insert the Ad Item
                    // Check if we have enough items to reach the 2nd row
                    if (finalItemsList.size >= 3) {
                        // Index 3 usually places it at the start of the 2nd row
                        // (Row 1 = Date Header + 2/3 images)
                        finalItemsList.add(3, LibraryItem.AdItem)
                    } else if (finalItemsList.isNotEmpty()) {
                        // If the list is short, just add it at the end
                        finalItemsList.add(LibraryItem.AdItem)
                    }

                    // 4. Update the state with the complete list
                    _allImagesState.value = LibraryImagesState.Success(finalItemsList)
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
