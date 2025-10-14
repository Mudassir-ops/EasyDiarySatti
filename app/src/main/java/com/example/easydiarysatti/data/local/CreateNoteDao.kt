package com.example.easydiarysatti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CreateNoteDao {

    @Insert
    suspend fun insertNote(note: CreateNoteEntity): Long

    @Update
    suspend fun updateNote(note: CreateNoteEntity)

    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: Long): CreateNoteEntity?

    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    fun observeNoteById(id: Long): Flow<CreateNoteEntity?>

    @Query(
        """
    SELECT * FROM create_note_entity_table
    ORDER BY 
        CASE WHEN (SELECT isAscending FROM create_note_entity_table LIMIT 1) = 1 THEN creationTime END ASC,
        CASE WHEN (SELECT isAscending FROM create_note_entity_table LIMIT 1) = 0 THEN creationTime END DESC
"""
    )
    fun observeAllNotes(): Flow<List<CreateNoteEntity>>


    @Query("SELECT images FROM create_note_entity_table WHERE noteId = :id")
    suspend fun getOldImages(id: Long): List<String>?


    @Query("SELECT * FROM create_note_entity_table")
    fun observeAllImages(): Flow<List<CreateNoteEntity>?>


    @Query("SELECT * FROM create_note_entity_table WHERE creationTime BETWEEN :startOfDay AND :endOfDay ORDER BY creationTime DESC")
    fun observeNotesForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<CreateNoteEntity>?>

    @Query(
        """
    UPDATE create_note_entity_table
    SET isAscending = NOT (
        COALESCE(
            (SELECT isAscending FROM create_note_entity_table LIMIT 1),
            0
        )
    )
"""
    )
    suspend fun toggleSortOrder()


    @Query("SELECT isAscending FROM create_note_entity_table LIMIT 1")
    fun observeSortOrder(): Flow<Boolean?>


}