package com.example.easydiarysatti.ui.dashboard

sealed class LibraryItem {
    data class DateItem(val date: String) : LibraryItem()
    data class ImagesItem(
        val date: String,
        val imagePaths: String,
        val noteTitle: String,
        val noteId: Long
    ) :
        LibraryItem()
}
