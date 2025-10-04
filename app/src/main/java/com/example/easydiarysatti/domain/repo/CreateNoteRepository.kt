package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity

interface CreateNoteRepository {
    suspend fun createEmptyNote(): Long
    suspend fun mergeAndSave(note: CreateNoteEntity)
    suspend fun getNoteById(id: Long): CreateNoteEntity?
    fun observeNoteById(id: Long): kotlinx.coroutines.flow.Flow<CreateNoteEntity?>
    fun observeAllNotes(): kotlinx.coroutines.flow.Flow<List<CreateNoteEntity>?>

    suspend fun getOldImages(noteId: Long): List<String>?
}