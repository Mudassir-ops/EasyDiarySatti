package com.example.easydiarysatti.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CreateNoteEntity::class, ReminderEntity::class, GlobalTagEntity::class],
    version = 23, exportSchema = true
)
@TypeConverters(ListStringConverter::class)
abstract class EasyDiaryDatabase : RoomDatabase() {
    abstract fun createNoteDao(): CreateNoteDao
    abstract fun globalTagDao(): GlobalTagDao
}