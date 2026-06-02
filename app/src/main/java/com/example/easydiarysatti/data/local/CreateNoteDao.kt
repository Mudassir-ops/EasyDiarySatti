package com.example.easydiarysatti.data.local

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun deleteNote(note: CreateNoteEntity)

    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    suspend fun getNoteById(id: Long): CreateNoteEntity?

    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    fun observeNoteById(id: Long): Flow<CreateNoteEntity?>

    // Excludes drafts — they must not appear in the main notes list
    @Query(
        """
        SELECT * FROM create_note_entity_table
        WHERE isDraft = 0
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

    // Excludes drafts — they must not appear on the calendar/day view
    @Query(
        """
        SELECT * FROM create_note_entity_table
        WHERE isDraft = 0
          AND creationTime BETWEEN :startOfDay AND :endOfDay
        ORDER BY creationTime DESC
        """
    )
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

    @Query("UPDATE create_note_entity_table SET tags = :newTags WHERE noteId = :noteId")
    suspend fun updateTagsForNote(
        noteId: Long,
        newTags: List<CustomTagEntity>
    )

    @Query("UPDATE create_note_entity_table SET images = :newImages WHERE noteId = :noteId")
    suspend fun updateImageForNote(
        noteId: Long,
        newImages: List<String>
    )

    // Returns only draft notes, newest first — used by the Drafts screen
    @Query("SELECT * FROM create_note_entity_table WHERE isDraft = 1 ORDER BY creationTime DESC")
    fun getDraftNotes(): Flow<List<CreateNoteEntity>>

    // Promotes a draft to a published note (clears the isDraft flag)
    @Query("UPDATE create_note_entity_table SET isDraft = 0 WHERE noteId = :noteId")
    suspend fun publishDraft(noteId: Long)

    ////---------

    @Insert
    suspend fun insertReminder(reminderEntity: ReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminderEntity: ReminderEntity)

    @Update
    suspend fun updateReminder(reminderEntity: ReminderEntity)

    @Query("SELECT * FROM reminder_table")
    fun observeReminder(): Flow<List<ReminderEntity>?>
}