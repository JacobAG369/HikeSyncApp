package com.example.hykesync.wear.presentation

/**
 * Representa el estado de la interfaz de usuario en el reloj.
 */
data class WearUiState(
    val heartRate: Float = 0f,
    val altitude: Float = 0f,
    val isSessionActive: Boolean = false,
    val sessionId: Long = 0L,
    val error: String? = null
)
