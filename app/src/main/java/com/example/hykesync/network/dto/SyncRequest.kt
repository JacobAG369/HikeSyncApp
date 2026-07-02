package com.example.hykesync.network.dto

data class SyncRequest(
    val sessionId: String,
    val telemetry: List<TelemetryDto>,
    val location: List<LocationDto>
)

data class TelemetryDto(
    val timestamp: Long,
    val heartRate: Float,
    val altitude: Float,
    val pressure: Float
)

data class LocationDto(
    val timestamp: Long,
    val latitud: Double,
    val longitud: Double
)
