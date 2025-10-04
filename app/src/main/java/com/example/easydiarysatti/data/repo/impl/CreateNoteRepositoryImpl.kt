package com.example.easydiarysatti.data.repo.impl

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import kotlinx.coroutines.flow.Flow

class CreateNoteRepositoryImpl(
    private val localDataSource: EasyDiaryLocalDataSource
) : CreateNoteRepository {

    override suspend fun createEmptyNote(): Long {
        return localDataSource.createEmptyNote()
    }

    override suspend fun mergeAndSave(note: CreateNoteEntity) {
        val existing = note.noteId.takeIf { it != 0L }?.let { localDataSource.getNoteById(it) }
        val merged = existing?.copy(
            title = note.title ?: existing.title,
            description = note.description ?: existing.description,
            feelingTitle = note.feelingTitle ?: existing.feelingTitle,
            feelingEmojiRes = note.feelingEmojiRes ?: existing.feelingEmojiRes,
            backgroundRes = note.backgroundRes ?: existing.backgroundRes,
            tags = note.tags ?: existing.tags,
            textColor = note.textColor ?: existing.textColor,
            textSizeH1 = note.textSizeH1 ?: existing.textSizeH1,
            textSizeH2 = note.textSizeH2 ?: existing.textSizeH2,
            textSizeH3 = note.textSizeH3 ?: existing.textSizeH3,
            textFont = note.textFont ?: existing.textFont,
            textAlignment = note.textAlignment ?: existing.textAlignment,
            text = note.text ?: existing.text,
            images = note.images ?: existing.images,
            creationTime = existing.creationTime
        ) ?: note
        if (existing == null) {
            localDataSource.insertNote(merged)
        } else {
            localDataSource.updateNote(merged)
        }
    }

    override suspend fun getNoteById(id: Long): CreateNoteEntity? {
        return localDataSource.getNoteById(id)
    }

    override fun observeNoteById(id: Long): Flow<CreateNoteEntity?> {
        return localDataSource.observeNoteById(id)
    }

    override fun observeAllNotes(): Flow<List<CreateNoteEntity>?> {
        return localDataSource.observeAllNotes()
    }

}