package com.example.easydiarysatti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CreateNoteDao {

    @Insert
    suspend fun insertNote(note: CreateNoteEntity): Long

    @Update
    suspend fun updateNote(note: CreateNoteEntity)

    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: Long): CreateNoteEntity?


}