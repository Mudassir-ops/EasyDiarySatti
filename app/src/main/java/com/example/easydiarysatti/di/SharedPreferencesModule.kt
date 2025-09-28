package com.example.easydiarysatti.di

import android.content.Context
import android.content.SharedPreferences
import com.example.easydiarysatti.PREF_NAME
import com.example.easydiarysatti.data.repo.SessionManagerRepoImpl
import com.example.easydiarysatti.domain.repo.SessionManagerRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedPreferencesModule {

    @Provides
    @Singleton
    fun provideSessionManagerRepository(sharedPreferences: SharedPreferences): SessionManagerRepo {
        return SessionManagerRepoImpl(sharedPreferences)
    }

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
