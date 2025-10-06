package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity
import kotlinx.coroutines.flow.Flow

interface CreateNoteRepository {
    suspend fun createEmptyNote(): Long
    suspend fun mergeAndSave(note: CreateNoteEntity)
    suspend fun getNoteById(id: Long): CreateNoteEntity?
    fun observeNoteById(id: Long): Flow<CreateNoteEntity?>
    fun observeAllNotes(): Flow<List<CreateNoteEntity>?>
    fun observeAllImages(): Flow<List<CreateNoteEntity>?>

    suspend fun getOldImages(noteId: Long): List<String>?
    fun observeNotesForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<CreateNoteEntity>?>

    suspend fun updateSortOrder(isAscending: Boolean)
}