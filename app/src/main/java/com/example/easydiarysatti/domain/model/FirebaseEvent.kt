package com.example.easydiarysatti.domain.model

data class FirebaseEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap()
)