package com.example.easydiarysatti.data.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity

interface EasyDiaryLocalDataSource {
    suspend fun createEmptyNote(): Long
    suspend fun insertNote(note: CreateNoteEntity): Long
    suspend fun updateNote(note: CreateNoteEntity)
    suspend fun getNoteById(id: Long): CreateNoteEntity?
    fun observeNoteById(id: Long): kotlinx.coroutines.flow.Flow<CreateNoteEntity?>
    suspend fun updateTitle(noteId: Long, title: String)
    suspend fun updateDescription(noteId: Long, description: String)
    suspend fun updateFeelingTitle(noteId: Long, feelingTitle: String)
    suspend fun updateFeelingEmoji(noteId: Long, emojiRes: Int)
    suspend fun updateBackground(noteId: Long, bgRes: Int)
    suspend fun updateTextColor(noteId: Long, color: String)
    suspend fun updateTextSizeH1(noteId: Long, size: Float)
    suspend fun updateTextSizeH2(noteId: Long, size: Float)
    suspend fun updateTextSizeH3(noteId: Long, size: Float)
    suspend fun updateTextFont(noteId: Long, font: String)
    suspend fun updateTextAlignment(noteId: Long, alignment: String)
    suspend fun updateText(noteId: Long, text: String)

    fun observeAllNotes(): kotlinx.coroutines.flow.Flow<List<CreateNoteEntity>?>
    suspend fun getOldImages(id: Long): List<String>?
}