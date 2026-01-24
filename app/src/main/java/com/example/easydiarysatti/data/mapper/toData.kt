package com.example.easydiarysatti.data.mapper

import com.example.easydiarysatti.data.local.CreateNoteEntity
import com.example.easydiarysatti.data.local.CustomTagEntity
import com.example.easydiarysatti.data.local.FirebaseEventData
import com.example.easydiarysatti.domain.model.FirebaseEvent
import com.example.easydiarysatti.domain.model.FirebaseNote

fun FirebaseEvent.toData(): FirebaseEventData {
    return FirebaseEventData(name = name, params = params)
}

fun FirebaseEventData.toDomain(): FirebaseEvent {
    return FirebaseEvent(name = name ?: "", params = params ?: emptyMap())
}

fun CreateNoteEntity.toFirebaseNote(): FirebaseNote {
    return FirebaseNote(
        noteId = noteId,
        title = title,
        description = description,
        isFavorite = isFavorite,
        feelingTitle = feelingTitle,
        feelingEmojiRes = feelingEmojiRes,
        backgroundRes = backgroundRes,
        selectedEmojiColor = selectedEmojiColor,
        textColor = textColor,
        tagColor = tagColor,
        textSizeHeader = textSizeHeader,
        textFont = textFont,
        textFontSize = textFontSize,
        textAlignment = textAlignment,
        text = text,
        creationTime = creationTime,
        tags = tags?.toFirebaseList(),
        images = images,
        isAscending = isAscending,
        remainderTime = remainderTime
    )
}

fun List<CustomTagEntity>.toFirebaseList(): List<Map<String, Any>> {
    return this.map { tag ->
        mapOf(
            "tagName" to tag.tagName,
            "noteId" to tag.noteId
        )
    }
}

fun FirebaseNote.toRoomEntity(): CreateNoteEntity {
    return CreateNoteEntity(
        noteId = noteId,
        title = title,
        description = description,
        isFavorite = isFavorite,
        feelingTitle = feelingTitle,
        feelingEmojiRes = feelingEmojiRes,
        backgroundRes = backgroundRes,
        selectedEmojiColor = selectedEmojiColor,
        textColor = textColor,
        tagColor = tagColor,
        textSizeHeader = textSizeHeader,
        textFont = textFont,
        textFontSize = textFontSize,
        textAlignment = textAlignment,
        text = text,
        creationTime = creationTime,
        tags = tags?.map { CustomTagEntity(it["tagName"] as String, it["noteId"] as Int) },
        images = images,
        isAscending = isAscending,
        remainderTime = remainderTime
    )
}
