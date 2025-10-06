package com.example.easydiarysatti.data.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "create_note_entity_table")
@TypeConverters(ListStringConverter::class)
data class CreateNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val noteId: Long = 0L,
    val title: String? = null,
    val description: String? = null,
    val feelingTitle: String? = null,
    val feelingEmojiRes: Int? = null,
    val backgroundRes: Int? = null,
    val textColor: String? = null,
    val tagColor: String? = null,
    val textSizeH1: Float? = null,
    val textSizeH2: Float? = null,
    val textSizeH3: Float? = null,
    val textFont: String? = null,
    val textAlignment: String? = null,
    val text: String? = null,
    val creationTime: Long = System.currentTimeMillis(),
    val tags: List<String>? = null,
    val images: List<String>? = null,
    val isAscending: Boolean = false
) : Parcelable

