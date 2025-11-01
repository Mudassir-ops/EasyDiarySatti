package com.example.easydiarysatti.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CreateNoteEntity::class, ReminderEntity::class],
    version = 16, exportSchema = false
)
@TypeConverters(ListStringConverter::class)
abstract class EasyDiaryDatabase : RoomDatabase() {
    abstract fun createNoteDao(): CreateNoteDao
}