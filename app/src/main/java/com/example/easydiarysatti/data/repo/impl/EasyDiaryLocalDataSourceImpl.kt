package com.example.easydiarysatti.data.repo.impl

import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource

class EasyDiaryLocalDataSourceImpl(
    private val dao: CreateNoteDao
) : EasyDiaryLocalDataSource {

    override suspend fun insertNote(note: CreateNoteEntity): Long {
        return dao.insertNote(note = note)
    }

    override suspend fun updateNote(note: CreateNoteEntity) {
        return dao.updateNote(note = note)
    }

    override suspend fun getNoteById(id: Long): CreateNoteEntity? {
        return dao.getNoteById(id = id)
    }

}