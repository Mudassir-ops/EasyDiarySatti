package com.example.easydiarysatti.data.repo.impl

import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.ReminderEntity
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class EasyDiaryLocalDataSourceImpl(
    private val dao: CreateNoteDao
) : EasyDiaryLocalDataSource {

    override suspend fun createEmptyNote(): Long {
        val emptyNote = CreateNoteEntity()
        return dao.insertNote(emptyNote)
    }

    override suspend fun deleteNote(note: CreateNoteEntity) {
        return dao.deleteNote(note)
    }

    override suspend fun insertNote(note: CreateNoteEntity): Long {
        return dao.insertNote(note = note)
    }

    override suspend fun updateNote(note: CreateNoteEntity) {
        return dao.updateNote(note = note)
    }

    override suspend fun getNoteById(id: Long): CreateNoteEntity? {
        return dao.getNoteById(id = id)
    }

    override fun observeNoteById(id: Long): Flow<CreateNoteEntity?> {
        return dao.observeNoteById(id = id)
    }

    override fun observeAllNotes(): Flow<List<CreateNoteEntity>?> {
        return dao.observeAllNotes().distinctUntilChanged()
    }

    override suspend fun getOldImages(id: Long): List<String>? {
        return dao.getOldImages(id = id)
    }

    override fun observeAllImages(): Flow<List<CreateNoteEntity>?> {
        return dao.observeAllImages().distinctUntilChanged()
    }

    override fun observeNotesForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<CreateNoteEntity>?> {
        return dao.observeNotesForDay(startOfDay = startOfDay, endOfDay = endOfDay)
    }

    override suspend fun updateSortOrder(isAscending: Boolean) {
        return dao.toggleSortOrder()
    }

    override fun observeSortOrder(): Flow<Boolean?> {
        return dao.observeSortOrder()
    }

    override suspend fun updateTagsForNote(
        noteId: Long,
        newTags: List<CustomTagEntity>
    ) {
        dao.updateTagsForNote(noteId = noteId, newTags = newTags)
    }

    override suspend fun updateImageForNote(
        noteId: Long,
        newImages: List<String>
    ) {
        dao.updateImageForNote(noteId = noteId, newImages = newImages)
    }

    // ── NEW ───────────────────────────────────────────────────────────────────

    override fun getDraftNotes(): Flow<List<CreateNoteEntity>> {
        return dao.getDraftNotes().distinctUntilChanged()
    }

    override suspend fun publishDraft(noteId: Long) {
        dao.publishDraft(noteId)
    }

    // ─────────────────────────────────────────────────────────────────────────

    override suspend fun insertReminder(reminderEntity: ReminderEntity): Long {
        return dao.insertReminder(reminderEntity)
    }

    override suspend fun deleteReminder(reminderEntity: ReminderEntity) {
        dao.deleteReminder(reminderEntity)
    }

    override suspend fun updateReminder(reminderEntity: ReminderEntity) {
        dao.updateReminder(reminderEntity)
    }

    override fun observeReminder(): Flow<List<ReminderEntity>?> {
        return dao.observeReminder()
    }
}