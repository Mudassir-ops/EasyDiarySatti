package com.example.easydiarysatti.remainder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_table")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reminderDate: String,
    val reminderTime: String,
    val description: String? = null,
    val scheduleAt: Long
)