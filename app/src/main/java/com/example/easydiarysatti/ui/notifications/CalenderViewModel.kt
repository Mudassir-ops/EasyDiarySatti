package com.example.easydiarysatti.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalenderViewModel @Inject constructor(
    private val repository: CreateNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalenderUiState(isLoading = true))
    val uiState: StateFlow<CalenderUiState> = _uiState

    fun loadMonth(month: YearMonth) {
        val startOfMonth =
            month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfMonth =
            month.atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.observeNotesForDay(startOfMonth, endOfMonth)
            .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
            .map { notes ->
                notes?.groupBy {
                    Instant.ofEpochMilli(it.creationTime).atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } ?: emptyMap()
            }
            .onEach { grouped ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    notesByDate = grouped
                )
            }
            .launchIn(viewModelScope)
    }

    fun selectDay(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDay = date)
    }

    fun currentDayNotes() {
        val today = LocalDate.now()
        selectDay(today)
    }

}


data class CalenderUiState(
    val isLoading: Boolean = false,
    val notesByDate: Map<LocalDate, List<CreateNoteEntity>> = emptyMap(),
    val selectedDay: LocalDate? = null
)