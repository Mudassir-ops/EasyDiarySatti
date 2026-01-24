package com.example.easydiarysatti.domain.model

data class Device(
    val serial: String,
    val type: DeviceType,
)

enum class DeviceType(val value: String) {
    MOBILE("mobile")
}