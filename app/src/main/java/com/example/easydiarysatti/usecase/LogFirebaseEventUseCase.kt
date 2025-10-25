package com.example.easydiarysatti.usecase

import com.example.easydiarysatti.domain.model.FirebaseEvent
import com.example.easydiarysatti.domain.repo.FirebaseEventLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogFirebaseEventUseCase @Inject constructor(
    private val eventLogger: FirebaseEventLogger
) {
    operator fun invoke(event: FirebaseEvent): Flow<Result<Unit>> {
        return eventLogger.logEvent(event)
    }
}