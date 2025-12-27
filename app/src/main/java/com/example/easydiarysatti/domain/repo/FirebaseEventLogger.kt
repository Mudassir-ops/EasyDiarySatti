package com.example.easydiarysatti.domain.repo

import com.example.easydiarysatti.domain.model.FirebaseEvent
import kotlinx.coroutines.flow.Flow

interface FirebaseEventLogger {
    fun logEvent(event: FirebaseEvent): Flow<Result<Unit>>

}