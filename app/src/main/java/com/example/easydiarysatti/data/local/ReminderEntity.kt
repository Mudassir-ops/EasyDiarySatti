package com.example.easydiarysatti.data.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@kotlinx.parcelize.Parcelize
@Entity(tableName = "reminder_table")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 0,
    val description: String? = null,
    val formattedDate: String? = null,
    val scheduleAt: Long,
    val shouldPlay: Boolean = false,
    val noteReminder: Boolean = false
) : Parcelable