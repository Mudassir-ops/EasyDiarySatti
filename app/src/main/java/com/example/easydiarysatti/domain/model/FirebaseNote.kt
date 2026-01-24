package com.example.easydiarysatti.domain.model

data class FirebaseNote(
    val noteId: Long = 0L,
    val title: String? = null,
    val description: String? = null,
    val isFavorite: Boolean = false,
    val feelingTitle: String? = null,
    val feelingEmojiRes: Int? = null,
    val backgroundRes: Int? = null,
    val selectedEmojiColor: String? = null,
    val textColor: Int? = null,
    val tagColor: String? = null,
    val textSizeHeader: Float? = null,
    val textFont: String? = null,
    val textFontSize: String? = null,
    val textAlignment: String? = null,
    val text: String? = null,
    val creationTime: Long = System.currentTimeMillis(),
    val tags: List<Map<String, Any>>? = null,
    val images: List<String>? = null,
    val isAscending: Boolean = false,
    val remainderTime: Long = System.currentTimeMillis()
)
