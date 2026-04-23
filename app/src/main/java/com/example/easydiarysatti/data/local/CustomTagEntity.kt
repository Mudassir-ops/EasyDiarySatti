package com.example.easydiarysatti.data.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomTagEntity(
    val tagName: String?,
    val noteId: Int
) : Parcelable