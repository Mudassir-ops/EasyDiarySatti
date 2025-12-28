package com.example.easydiarysatti.di

import android.content.Context
import androidx.room.Room
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
            context.applicationContext,
            EasyDiaryDatabase::class.java,
            "easy_diary_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideNoteDao(database: EasyDiaryDatabase): CreateNoteDao {
        return database.createNoteDao()
    }
}
