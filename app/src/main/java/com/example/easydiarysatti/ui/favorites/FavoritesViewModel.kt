package com.example.easydiarysatti.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _favoritesState = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val favoritesState: StateFlow<FavoritesState> = _favoritesState

    init {
        observeFavorites()
    }

    private fun observeFavorites() {
        createNoteRepository.observeAllNotes()
            .onStart {
                _favoritesState.value = FavoritesState.Loading
            }
            .catch { e ->
                _favoritesState.value = FavoritesState.Error(e.message ?: "Unknown error")
            }
            .onEach { notes ->
                val favorites = notes?.filter { it.isFavorite } ?: emptyList()
                if (favorites.isEmpty()) {
                    _favoritesState.value = FavoritesState.Empty
                } else {
                    _favoritesState.value = FavoritesState.Success(favorites)
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleFavorite(note: CreateNoteEntity) {
        viewModelScope.launch {
            val updatedNote = note.copy(isFavorite = !note.isFavorite)
            createNoteRepository.updateNote(updatedNote)
        }
    }
}

sealed interface FavoritesState {
    object Loading : FavoritesState
    object Empty : FavoritesState
    data class Success(val favorites: List<CreateNoteEntity>) : FavoritesState
    data class Error(val message: String) : FavoritesState
}