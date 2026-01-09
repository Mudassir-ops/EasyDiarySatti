package com.example.easydiarysatti.data.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

interface EasyDiaryLocalDataSource {
    suspend fun createEmptyNote(): Long
    suspend fun insertNote(note: CreateNoteEntity): Long
    suspend fun updateNote(note: CreateNoteEntity)
    suspend fun getNoteById(id: Long): CreateNoteEntity?
    fun observeNoteById(id: Long): Flow<CreateNoteEntity?>
    fun observeAllNotes(): Flow<List<CreateNoteEntity>?>
    suspend fun getOldImages(id: Long): List<String>?
    fun observeAllImages(): Flow<List<CreateNoteEntity>?>

    fun observeNotesForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<CreateNoteEntity>?>

    suspend fun updateSortOrder(isAscending: Boolean)
    fun observeSortOrder(): Flow<Boolean?>
    suspend fun updateTagsForNote(
        noteId: Long,
        newTags: List<CustomTagEntity>
    )

    suspend fun updateImageForNote(
        noteId: Long,
        newImages: List<String>
    )

    suspend fun deleteNote(note: CreateNoteEntity)
    suspend fun insertReminder(reminderEntity: ReminderEntity): Long
    suspend fun deleteReminder(reminderEntity: ReminderEntity)
    suspend fun updateReminder(reminderEntity: ReminderEntity)
    fun observeReminder(): Flow<List<ReminderEntity>?>

}