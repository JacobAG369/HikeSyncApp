package com.example.hykesync.presentation

/**
 * Representa el estado de la interfaz de usuario en el teléfono.
 */
data class AppUiState(
    val connectionStatus: String = "Desconectado",
    val linkedDeviceName: String = "Sin dispositivo vinculado",
    val isWearDeviceReachable: Boolean = false,
    val latestTelemetryConnectionStatus: String = "Sin telemetria reciente",
    val lastHeartRate: Float = 0f,
    val lastAltitude: Float = 0f,
    val lastAzimuth: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isSessionActive: Boolean = false,
    val sessionId: Long = 0L,
    val routeHistory: List<RouteHistoryItem> = emptyList(),
    val selectedRouteDetail: RouteDetailUiState? = null,
    val isRouteDetailLoading: Boolean = false,
    val routeDetailError: String? = null,
    val error: String? = null
)

data class RouteHistoryItem(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    val status: String,
    val telemetryCount: Int,
    val locationCount: Int,
    val lastHeartRate: Float?,
    val lastAltitude: Float?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
)

data class RouteDetailUiState(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long?,
    val status: String,
    val durationMillis: Long,
    val telemetryCount: Int,
    val locationCount: Int,
    val averageHeartRate: Float?,
    val maxHeartRate: Float?,
    val minAltitude: Float?,
    val maxAltitude: Float?,
    val distanceKm: Float,
    val elevationGainMeters: Float,
    val elevationLossMeters: Float,
    val averageSpeedKmh: Float?,
    val heartRateSeries: List<Float>,
    val altitudeSeries: List<Float>,
    val routePoints: List<RouteMapPoint>,
)

data class RouteMapPoint(
    val latitude: Double,
    val longitude: Double,
)
