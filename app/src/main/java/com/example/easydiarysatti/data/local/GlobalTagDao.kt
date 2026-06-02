package com.example.easydiarysatti.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalTagDao {

    /** Live list — emits automatically on every insert / delete / rename. */
    @Query("SELECT * FROM global_tags ORDER BY id DESC")
    fun observeAllTags(): Flow<List<GlobalTagEntity>>

    /** Insert; silently ignores if the same name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: GlobalTagEntity)

    @Query("DELETE FROM global_tags WHERE tagName = :name COLLATE NOCASE")
    suspend fun deleteTag(name: String)

    @Query("UPDATE global_tags SET tagName = :newName WHERE tagName = :oldName COLLATE NOCASE")
    suspend fun renameTag(oldName: String, newName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM global_tags WHERE tagName = :name COLLATE NOCASE)")
    suspend fun exists(name: String): Boolean
}