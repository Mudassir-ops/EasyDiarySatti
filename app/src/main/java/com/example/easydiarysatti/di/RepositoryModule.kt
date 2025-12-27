package com.example.easydiarysatti.di

import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import com.example.easydiarysatti.data.repo.impl.CreateNoteRepositoryImpl
import com.example.easydiarysatti.data.repo.impl.EasyDiaryLocalDataSourceImpl
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideEasyDiaryLocalDataSource(dao: CreateNoteDao): EasyDiaryLocalDataSource {
        return EasyDiaryLocalDataSourceImpl(dao = dao)
    }


    @Provides
    @Singleton
    fun provideCreateNoteRepository(localDataSource: EasyDiaryLocalDataSource): CreateNoteRepository {
        return CreateNoteRepositoryImpl(localDataSource = localDataSource)
    }

}