package com.example.easydiarysatti.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.local.EasyDiaryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    @Provides
    @Singleton
    fun provideNotepadDatabase(@ApplicationContext context: Context): EasyDiaryDatabase {
        return Room.databaseBuilder(
            context,
            EasyDiaryDatabase::class.java,
            "easy_diary_database"
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigrationOnDowngrade()
            .fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: EasyDiaryDatabase): CreateNoteDao {
        return database.createNoteDao()
    }
}
