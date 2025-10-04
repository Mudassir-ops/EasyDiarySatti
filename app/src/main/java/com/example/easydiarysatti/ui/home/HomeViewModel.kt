package com.example.easydiarysatti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import com.example.easydiarysatti.data.repo.impl.EasyDiaryLocalDataSourceImpl
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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

    init {
        observeAllNotes()
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

}

sealed interface HomeNotesState {
    object Loading : HomeNotesState
    data class Success(val notes: List<CreateNoteEntity>) : HomeNotesState
    data class Error(val message: String) : HomeNotesState
}