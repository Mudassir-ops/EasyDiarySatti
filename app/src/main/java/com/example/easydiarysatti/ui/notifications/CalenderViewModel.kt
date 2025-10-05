package com.example.easydiarysatti.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.ui.home.HomeNotesState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class CalenderViewModel @Inject constructor(
    private val repository: CreateNoteRepository
) : ViewModel() {

    private val _allCalenderNotesState = MutableStateFlow<HomeNotesState>(HomeNotesState.Loading)
    val allCalenderNotesState: StateFlow<HomeNotesState> = _allCalenderNotesState
    private var observeJob: Job? = null

    fun observeAllCalenderNotes(
        startOfDay: Long,
        endOfDay: Long
    ) {
        Log.e("currentDay", "setupCalender: $startOfDay--$endOfDay")
        observeJob?.cancel()
        _allCalenderNotesState.value = HomeNotesState.Loading
        observeJob = repository.observeNotesForDay(startOfDay = startOfDay, endOfDay = endOfDay)
            .onStart {
                _allCalenderNotesState.value = HomeNotesState.Loading
            }
            .catch { e ->
                _allCalenderNotesState.value = HomeNotesState.Error(e.message ?: "Unknown error")
            }
            .onEach { notes ->
                if (notes?.isEmpty() == true) {
                    _allCalenderNotesState.value =
                        HomeNotesState.Error("No notes found")
                } else {
                    notes?.let { _allCalenderNotesState.value = HomeNotesState.Success(it) }
                }
            }
            .launchIn(viewModelScope)
    }

}

sealed interface CalenderNotesState {
    object Loading : CalenderNotesState
    data class Success(val notes: List<CreateNoteEntity>) : CalenderNotesState
    data class Error(val message: String) : CalenderNotesState
}