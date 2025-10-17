package com.example.easydiarysatti.data.repo.impl

import android.util.Log
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
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
        Log.e("headerSaveSatti", "setClickListeners:$existing ")
        val merged = existing?.copy(
            title = note.title ?: existing.title,
            description = note.description ?: existing.description,
            feelingTitle = note.feelingTitle ?: existing.feelingTitle,
            feelingEmojiRes = note.feelingEmojiRes ?: existing.feelingEmojiRes,
            backgroundRes = note.backgroundRes ?: existing.backgroundRes,
            tags = note.tags ?: existing.tags,
            selectedEmojiColor = note.selectedEmojiColor ?: existing.selectedEmojiColor,
            textFont = note.textFont ?: existing.textFont,
            textAlignment = note.textAlignment ?: existing.textAlignment,
            text = note.text ?: existing.text,
            images = note.images ?: existing.images,
            creationTime = existing.creationTime,
            tagColor = note.tagColor ?: existing.tagColor,
            textColor = note.textColor ?: existing.textColor,
            textSizeHeader = note.textSizeHeader ?: existing.textSizeHeader,
            textFontSize = note.textFontSize ?: existing.textFontSize
        ) ?: note
        if (existing == null) {
            val newNoteId = localDataSource.insertNote(merged)
            Log.e("headerSaveSatti", "39->setClickListeners:$newNoteId-->$merged ")
            val updatedTags = if (merged.tags.isNullOrEmpty()) {
                listOf(
                    CustomTagEntity(
                        noteId = newNoteId.toInt(),
                        tagName = "Personal"
                    )
                )
            } else {
                merged.tags.map { it.copy(noteId = newNoteId.toInt()) }
            }
            Log.e("headerSaveSattiSAAAA", "39->setClickListeners:$newNoteId-->$updatedTags ")
            if (updatedTags.isNotEmpty()) {
                localDataSource.updateNote(
                    merged.copy(tags = updatedTags)
                )
            }
        } else {
            Log.e("headerSaveSatti", "42->setClickListeners:$existing ")
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

    override fun observeAllImages(): Flow<List<CreateNoteEntity>?> {
        return localDataSource.observeAllImages()
    }

    override suspend fun getOldImages(noteId: Long): List<String>? {
        return localDataSource.getOldImages(id = noteId)
    }

    override fun observeNotesForDay(
        startOfDay: Long, endOfDay: Long
    ): Flow<List<CreateNoteEntity>?> {
        return localDataSource.observeNotesForDay(startOfDay = startOfDay, endOfDay = endOfDay)
    }

    override suspend fun updateSortOrder(isAscending: Boolean) {
        return localDataSource.updateSortOrder(isAscending)
    }

    override fun observeSortOrder(): Flow<Boolean?> {
        return localDataSource.observeSortOrder()
    }

    override suspend fun updateTagsForNote(
        noteId: Long, newTags: List<CustomTagEntity>
    ) {
        localDataSource.updateTagsForNote(noteId = noteId, newTags = newTags)
    }


}