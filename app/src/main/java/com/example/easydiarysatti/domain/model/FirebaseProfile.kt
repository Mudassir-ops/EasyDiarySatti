package com.example.easydiarysatti.domain.model

data class FirebaseProfile(
    val name: String? = null,
    val profilePicUrl: String? = null,
    val email: String? = null,
    val pinHash: String? = null,
    val theme: Int? = null
)
