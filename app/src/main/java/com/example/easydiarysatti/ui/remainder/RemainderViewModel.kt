package com.example.easydiarysatti.ui.remainder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemainderViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    private val _reminderState = MutableStateFlow<ReminderState?>(null)
    val reminderState: StateFlow<ReminderState?> = _reminderState.asStateFlow()


    fun insertReminder(reminderEntity: ReminderEntity) {
        viewModelScope.launch {
            createNoteRepository.insertReminder(reminderEntity = reminderEntity)
        }
    }

    fun deleteReminder(reminderEntity: ReminderEntity) {
        viewModelScope.launch {
            createNoteRepository.deleteReminder(reminderEntity = reminderEntity)
        }
    }

    fun updateReminder(reminderEntity: ReminderEntity) {
        viewModelScope.launch {
            createNoteRepository.updateReminder(reminderEntity = reminderEntity)
        }
    }

    fun observeReminders() {
        viewModelScope.launch {
            createNoteRepository.observeReminder().collect { reminders ->
                val noteReminders = reminders?.filter { it.noteReminder } ?: listOf()
                val dailyReminders = reminders?.filterNot { it.noteReminder } ?: listOf()
                _reminderState.value = ReminderState(
                    noteReminders = noteReminders,
                    dailyReminders = dailyReminders
                )
            }
        }
    }

}

data class ReminderState(
    val noteReminders: List<ReminderEntity> = emptyList(),
    val dailyReminders: List<ReminderEntity> = emptyList()
)