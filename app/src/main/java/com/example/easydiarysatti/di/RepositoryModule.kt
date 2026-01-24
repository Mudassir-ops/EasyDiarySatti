package com.example.easydiarysatti.di

import android.content.Context
import com.example.easydiarysatti.data.local.CreateNoteDao
import com.example.easydiarysatti.data.repo.EasyDiaryLocalDataSource
import com.example.easydiarysatti.data.repo.FirebaseSessionRepoImpl
import com.example.easydiarysatti.data.repo.impl.CreateNoteRepositoryImpl
import com.example.easydiarysatti.data.repo.impl.EasyDiaryLocalDataSourceImpl
import com.example.easydiarysatti.domain.model.Device
import com.example.easydiarysatti.domain.model.DeviceType
import com.example.easydiarysatti.domain.repo.CreateNoteRepository
import com.example.easydiarysatti.domain.repo.FirebaseRemoteDataSync
import com.example.easydiarysatti.getDeviceSerialNumber
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideCreateNoteRepository(
        localDataSource: EasyDiaryLocalDataSource,
        firebaseRemoteDataSync: FirebaseRemoteDataSync
    ): CreateNoteRepository {
        return CreateNoteRepositoryImpl(
            localDataSource = localDataSource,
            firebaseRepo = firebaseRemoteDataSync
        )
    }

    @Provides
    fun provideFirebaseSessionRepo(
        device: Device
    ): FirebaseRemoteDataSync =
        FirebaseSessionRepoImpl(
            database = FirebaseDatabase.getInstance().reference,
            device = device
        )

    @Provides
    @Singleton
    fun provideDevice(@ApplicationContext context: Context): Device {
        val serialNumber = context.getDeviceSerialNumber()
        return Device(serialNumber, DeviceType.MOBILE)
    }

}