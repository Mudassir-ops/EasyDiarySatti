package com.example.easydiarysatti.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "global_tags")
data class GlobalTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tagName: String
)