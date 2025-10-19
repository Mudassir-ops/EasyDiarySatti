package com.example.easydiarysatti.data.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@kotlinx.parcelize.Parcelize
@Entity(tableName = "reminder_table")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val reminderDate: String,
    val reminderTime: String,
    val description: String? = null,
    val scheduleAt: Long,
    val shouldPlay: Boolean = false,
    val noteReminder: Boolean = false,
) : Parcelable