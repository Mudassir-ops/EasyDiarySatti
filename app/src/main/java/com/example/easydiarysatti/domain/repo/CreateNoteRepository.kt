package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.ReminderEntity
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
    fun observeSortOrder(): Flow<Boolean?>
    suspend fun updateNote(note: CreateNoteEntity)
    suspend fun updateTagsForNote(
        noteId: Long,
        newTags: List<CustomTagEntity>
    )
    suspend fun deleteNote(note: CreateNoteEntity)
    suspend fun updateImageForNote(
        noteId: Long,
        newImages: List<String>
    )


    suspend fun insertReminder(reminderEntity: ReminderEntity?): Long?
    suspend fun deleteReminder(reminderEntity: ReminderEntity?)
    suspend fun updateReminder(reminderEntity: ReminderEntity?)
    fun observeReminder(): Flow<List<ReminderEntity>?>

}