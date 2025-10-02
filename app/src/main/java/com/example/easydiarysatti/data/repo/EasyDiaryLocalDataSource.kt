package com.example.easydiarysatti.data.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity

interface EasyDiaryLocalDataSource {
    suspend fun insertNote(note: CreateNoteEntity): Long
    suspend fun updateNote(note: CreateNoteEntity)
    suspend fun getNoteById(id: Long): CreateNoteEntity?
}