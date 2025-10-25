package com.example.easydiarysatti.utills

import android.util.Log
import com.example.easydiarysatti.domain.model.FirebaseEvent
import com.example.easydiarysatti.usecase.LogFirebaseEventUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object AppEventLogger {
    private var loggerUseCase: LogFirebaseEventUseCase? = null
    fun init(useCase: LogFirebaseEventUseCase) {
        loggerUseCase = useCase
    }

    fun CoroutineScope.logEventWithScope(
        name: String, params: Map<String, Any> = emptyMap()
    ) {
        loggerUseCase?.invoke(
            FirebaseEvent(name, params)
        )?.onEach {
            it.onSuccess {
                Log.d("AppEventLogger", "Logged: $name")
            }.onFailure { ex ->
                Log.e("AppEventLogger", "Failed to log event: $name", ex)
            }
        }?.launchIn(this)
    }
}
