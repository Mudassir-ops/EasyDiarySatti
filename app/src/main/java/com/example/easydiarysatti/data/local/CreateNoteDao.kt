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

    // observe single note
    @Query("SELECT * FROM create_note_entity_table WHERE noteId = :id LIMIT 1")
    fun observeNoteById(id: Long): kotlinx.coroutines.flow.Flow<CreateNoteEntity?>

    @Query("UPDATE create_note_entity_table SET title = :title WHERE noteId = :noteId")
    suspend fun updateTitle(noteId: Long, title: String)

    @Query("UPDATE create_note_entity_table SET description = :description WHERE noteId = :noteId")
    suspend fun updateDescription(noteId: Long, description: String)

    @Query("UPDATE create_note_entity_table SET feelingTitle = :feelingTitle WHERE noteId = :noteId")
    suspend fun updateFeelingTitle(noteId: Long, feelingTitle: String)

    @Query("UPDATE create_note_entity_table SET feelingEmojiRes = :emojiRes WHERE noteId = :noteId")
    suspend fun updateFeelingEmoji(noteId: Long, emojiRes: Int)

    @Query("UPDATE create_note_entity_table SET backgroundRes = :bgRes WHERE noteId = :noteId")
    suspend fun updateBackground(noteId: Long, bgRes: Int)

    @Query("UPDATE create_note_entity_table SET textColor = :color WHERE noteId = :noteId")
    suspend fun updateTextColor(noteId: Long, color: String)

    @Query("UPDATE create_note_entity_table SET textSizeH1 = :size WHERE noteId = :noteId")
    suspend fun updateTextSizeH1(noteId: Long, size: Float)

    @Query("UPDATE create_note_entity_table SET textSizeH2 = :size WHERE noteId = :noteId")
    suspend fun updateTextSizeH2(noteId: Long, size: Float)

    @Query("UPDATE create_note_entity_table SET textSizeH3 = :size WHERE noteId = :noteId")
    suspend fun updateTextSizeH3(noteId: Long, size: Float)

    @Query("UPDATE create_note_entity_table SET textFont = :font WHERE noteId = :noteId")
    suspend fun updateTextFont(noteId: Long, font: String)

    @Query("UPDATE create_note_entity_table SET textAlignment = :alignment WHERE noteId = :noteId")
    suspend fun updateTextAlignment(noteId: Long, alignment: String)

    @Query("UPDATE create_note_entity_table SET text = :text WHERE noteId = :noteId")
    suspend fun updateText(noteId: Long, text: String)

    @Query("UPDATE create_note_entity_table SET tags = :tags WHERE noteId = :noteId")
    suspend fun updateTags(noteId: Long, tags: List<String>)

    @Query("UPDATE create_note_entity_table SET images = :images WHERE noteId = :noteId")
    suspend fun updateImages(noteId: Long, images: List<String>)

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