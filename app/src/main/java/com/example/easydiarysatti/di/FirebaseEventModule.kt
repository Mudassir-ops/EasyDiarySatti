package com.example.easydiarysatti.di

import android.content.Context
import com.example.easydiarysatti.data.repo.FirebaseEventLoggerImpl
import com.example.easydiarysatti.domain.repo.FirebaseEventLogger
import com.example.easydiarysatti.source.FirebaseAnalyticsDataSource
import com.example.easydiarysatti.usecase.LogFirebaseEventUseCase
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseEventModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    @Provides
    fun provideFirebaseAnalyticsDataSource(
        firebaseAnalytics: FirebaseAnalytics
    ) = FirebaseAnalyticsDataSource(firebaseAnalytics)

    @Provides
    fun provideFirebaseEventLogger(
        impl: FirebaseEventLoggerImpl
    ): FirebaseEventLogger = impl

    @Provides
    fun provideLogEventUseCase(
        eventLogger: FirebaseEventLogger
    ): LogFirebaseEventUseCase = LogFirebaseEventUseCase(eventLogger)

}