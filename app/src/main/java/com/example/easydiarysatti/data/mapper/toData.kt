package com.example.easydiarysatti.data.mapper

import com.example.easydiarysatti.domain.model.FirebaseEvent
import com.example.easydiarysatti.data.local.FirebaseEventData

fun FirebaseEvent.toData(): FirebaseEventData {
    return FirebaseEventData(name = name, params = params)
}

fun FirebaseEventData.toDomain(): FirebaseEvent {
    return FirebaseEvent(name = name ?: "", params = params ?: emptyMap())
}
