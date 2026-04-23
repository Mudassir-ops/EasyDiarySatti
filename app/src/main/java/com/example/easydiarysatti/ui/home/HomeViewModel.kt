package com.example.easydiarysatti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.GlobalTagDao
import com.example.easydiarysatti.data.local.GlobalTagEntity
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val createNoteRepository: CreateNoteRepository,
    private val sessionManagerRepo: SessionManagerRepo,
    private val globalTagDao: GlobalTagDao          // ← injected Room DAO
) : ViewModel() {

    private val _allNotesState = MutableStateFlow<HomeNotesState>(HomeNotesState.Loading)
    val allNotesState: StateFlow<HomeNotesState> = _allNotesState

    private val _sortOrder = MutableStateFlow<Boolean?>(false)
    val sortOrder: StateFlow<Boolean?> = _sortOrder

    // ── Global tags — now a live Room Flow ────────────────────────────────────
    //
    //  globalTagsState emits List<String> (tag names only) so AddTagsFragment
    //  doesn't need to know about GlobalTagEntity.
    //  Room emits automatically on every insert / delete / rename — no manual
    //  StateFlow updates needed anywhere.
    val globalTagsState = globalTagDao.observeAllTags()
        .map { list -> list.map { it.tagName } }    // expose names only

    var currentSortOrder = false

    init {
        observeAllNotes()
        observeSortOrders()
        migrateSharedPrefTagsToDb()   // one-time migration from SharedPrefs
    }

    fun observeAllNotes() {
        createNoteRepository.observeAllNotes()
            .onStart { _allNotesState.value = HomeNotesState.Loading }
            .catch { e -> _allNotesState.value = HomeNotesState.Error(e.message ?: "Unknown error") }
            .onEach { notes ->
                if (notes?.isEmpty() == true) {
                    _allNotesState.value = HomeNotesState.Error("No notes found")
                } else {
                    notes?.let { _allNotesState.value = HomeNotesState.Success(it) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun observeSortOrders() {
        viewModelScope.launch {
            createNoteRepository.observeSortOrder().distinctUntilChanged()
                .catch { e -> e.printStackTrace() }
                .onEach { notes -> _sortOrder.value = notes }
                .launchIn(viewModelScope)
        }
    }

    fun updateSortOrder() {
        viewModelScope.launch {
            createNoteRepository.updateSortOrder(isAscending = false)
        }
    }

    fun deleteNote(note: CreateNoteEntity) {
        viewModelScope.launch { createNoteRepository.deleteNote(note) }
    }

    fun toggleFavorite(note: CreateNoteEntity) {
        viewModelScope.launch {
            createNoteRepository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    // ── Global tag management — all backed by Room ────────────────────────────

    /** Insert tag if it doesn't already exist. Room flow emits → grid updates. */
    fun saveGlobalTag(tagName: String) {
        viewModelScope.launch {
            globalTagDao.insertTag(GlobalTagEntity(tagName = tagName))
        }
    }

    /** Delete tag by name. Room flow emits → grid updates. */
    fun removeGlobalTag(tagName: String) {
        viewModelScope.launch { globalTagDao.deleteTag(tagName) }
    }

    /** Rename tag. Room flow emits → grid updates. */
    fun renameGlobalTag(oldName: String, newName: String) {
        viewModelScope.launch { globalTagDao.renameTag(oldName, newName) }
    }

    // ── One-time SharedPrefs → Room migration ─────────────────────────────────
    //
    //  Runs once on first launch after the DB upgrade.
    //  Reads any tags previously saved in SharedPrefs and inserts them into
    //  the new global_tags table, then clears the SharedPrefs entry so this
    //  migration never runs again.

    private fun migrateSharedPrefTagsToDb() {
        viewModelScope.launch {
            val legacy = sessionManagerRepo.getGlobalTags()
            if (legacy.isNotEmpty()) {
                legacy.forEach { name ->
                    if (name.isNotBlank()) {
                        globalTagDao.insertTag(GlobalTagEntity(tagName = name))
                    }
                }
                // Clear SharedPrefs so migration doesn't re-run
                sessionManagerRepo.saveGlobalTags(emptyList())
            }
        }
    }

    fun duplicateNote(note: CreateNoteEntity) {
      viewModelScope.launch {
          createNoteRepository.mergeAndSave(
              note.copy(
                  noteId = 0,
                  title  = "${note.title} (copy)"
              )
          )
      }
  }
}

sealed interface HomeNotesState {
    object Loading : HomeNotesState
    data class Success(val notes: List<CreateNoteEntity>?) : HomeNotesState
    data class Error(val message: String) : HomeNotesState
}