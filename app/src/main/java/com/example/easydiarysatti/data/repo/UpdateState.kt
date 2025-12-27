package com.example.easydiarysatti.data.repo

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Downloaded : UpdateState
    data object Installing : UpdateState
    data object Completed : UpdateState
    data object Failed : UpdateState
    data object Cancelled : UpdateState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : UpdateState
    data class UpdateAvailable(
        val isImmediate: Boolean,
        val stalenessDays: Int?,
        val priority: Int
    ) : UpdateState
}