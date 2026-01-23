package com.example.easydiarysatti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _allNotesState = MutableStateFlow<HomeNotesState>(HomeNotesState.Loading)
    val allNotesState: StateFlow<HomeNotesState> = _allNotesState

    private val _sortOrder = MutableStateFlow<Boolean?>(false)
    val sortOrder: StateFlow<Boolean?> = _sortOrder

    var currentSortOrder = false

    init {
        observeAllNotes()
        observeSortOrders()
    }

    fun observeAllNotes() {
        createNoteRepository.observeAllNotes()
            .onStart {
                _allNotesState.value = HomeNotesState.Loading
            }
            .catch { e ->
                _allNotesState.value = HomeNotesState.Error(e.message ?: "Unknown error")
            }
            .onEach { notes ->
                if (notes?.isEmpty() == true) {
                    _allNotesState.value =
                        HomeNotesState.Error("No notes found")
                } else {
                    notes?.let { _allNotesState.value = HomeNotesState.Success(it) }
                }
            }
            .launchIn(viewModelScope)
    }


    fun observeSortOrders() {
        viewModelScope.launch {
            createNoteRepository.observeSortOrder().distinctUntilChanged()
                .onStart {
                }
                .catch { e ->
                    e.printStackTrace()
                }
                .onEach { notes ->
                    _sortOrder.value = notes
                }
                .launchIn(viewModelScope)
        }
    }

    fun updateSortOrder() {
        viewModelScope.launch {
            createNoteRepository.updateSortOrder(isAscending = false)
        }
    }
    fun deleteNote(note: CreateNoteEntity) {
        viewModelScope.launch {
            createNoteRepository.deleteNote(note)
        }
    }
    fun toggleFavorite(note: CreateNoteEntity) {
        viewModelScope.launch {
            // Toggle the boolean and update database
            val updatedNote = note.copy(isFavorite = !note.isFavorite)
            createNoteRepository.updateNote(updatedNote)
        }
    }
}

sealed interface HomeNotesState {
    object Loading : HomeNotesState
    data class Success(val notes: List<CreateNoteEntity>?) : HomeNotesState
    data class Error(val message: String) : HomeNotesState
}