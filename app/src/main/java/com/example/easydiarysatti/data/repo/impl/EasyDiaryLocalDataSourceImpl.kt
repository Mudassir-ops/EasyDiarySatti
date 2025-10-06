package com.example.easydiarysatti.data.repo.impl

import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.local.CreateNoteEntity
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

    override suspend fun updateTitle(noteId: Long, title: String) = dao.updateTitle(noteId, title)
    override suspend fun updateDescription(noteId: Long, description: String) =
        dao.updateDescription(noteId, description)

    override suspend fun updateFeelingTitle(noteId: Long, feelingTitle: String) =
        dao.updateFeelingTitle(noteId, feelingTitle)

    override suspend fun updateFeelingEmoji(noteId: Long, emojiRes: Int) =
        dao.updateFeelingEmoji(noteId, emojiRes)

    override suspend fun updateBackground(noteId: Long, bgRes: Int) =
        dao.updateBackground(noteId, bgRes)

    override suspend fun updateTextColor(noteId: Long, color: String) =
        dao.updateTextColor(noteId, color)

    override suspend fun updateTextSizeH1(noteId: Long, size: Float) =
        dao.updateTextSizeH1(noteId, size)

    override suspend fun updateTextSizeH2(noteId: Long, size: Float) =
        dao.updateTextSizeH2(noteId, size)

    override suspend fun updateTextSizeH3(noteId: Long, size: Float) =
        dao.updateTextSizeH3(noteId, size)

    override suspend fun updateTextFont(noteId: Long, font: String) =
        dao.updateTextFont(noteId, font)

    override suspend fun updateTextAlignment(noteId: Long, alignment: String) =
        dao.updateTextAlignment(noteId, alignment)

    override suspend fun updateText(noteId: Long, text: String) = dao.updateText(noteId, text)

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
       return dao.updateSortOrder(isAscending)
    }

}