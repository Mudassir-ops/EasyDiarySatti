package com.example.easydiarysatti.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftNotesViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository
) : ViewModel() {

    // ── UI State ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<DraftUiState>(DraftUiState.Loading)
    val uiState: StateFlow<DraftUiState> = _uiState

    // ── Init ─────────────────────────────────────────────────────────────────

    init {
        loadDrafts()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun loadDrafts() {
        viewModelScope.launch {
            // observeDraftNotes() returns a live Room Flow — list updates automatically
            // whenever a draft is saved or deleted, no manual refresh needed.
            createNoteRepository.observeDraftNotes().collect { drafts ->
                _uiState.update {
                    if (drafts.isEmpty()) DraftUiState.Empty
                    else DraftUiState.Success(drafts)
                }
            }
        }
    }

    fun deleteDraft(noteId: Long) {
        viewModelScope.launch {
            // deleteNote requires a full entity; we use publishDraft as a proxy
            // to clear the row, or better — delete directly via the note object.
            // Since the Repository only has deleteNote(entity), we fetch first.
            val note = createNoteRepository.getNoteById(noteId)
            if (note != null) {
                createNoteRepository.deleteNote(note)
            }
        }
    }

    /**
     * Promotes a draft to a published note by clearing the isDraft flag.
     * The caller must pass the updated entity into CreateNotesViewModel.mergeAndSave()
     * to actually persist the change.
     */
    fun clearDraftFlag(noteEntity: CreateNoteEntity): CreateNoteEntity =
        noteEntity.copy(isDraft = false)
}

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface DraftUiState {
    data object Loading : DraftUiState
    data object Empty   : DraftUiState
    data class  Success(val drafts: List<CreateNoteEntity>) : DraftUiState
}