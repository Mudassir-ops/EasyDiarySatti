package com.example.easydiarysatti.data.repo

import com.example.easydiarysatti.data.mapper.toData
import com.example.easydiarysatti.domain.model.FirebaseEvent
import com.example.easydiarysatti.domain.repo.FirebaseEventLogger
import com.example.easydiarysatti.source.FirebaseAnalyticsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class FirebaseEventLoggerImpl @Inject constructor(
    private val dataSource: FirebaseAnalyticsDataSource
) : FirebaseEventLogger {

    override fun logEvent(event: FirebaseEvent): Flow<Result<Unit>> = flow {
        try {
            val eventData = event.toData()
            dataSource.logEvent(eventData.name ?: "", eventData.params ?: emptyMap())
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

}