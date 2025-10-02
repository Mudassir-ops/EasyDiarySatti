package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.data.local.CreateNoteEntity

interface CreateNoteRepository {
    suspend fun mergeAndSave(note: CreateNoteEntity)
}