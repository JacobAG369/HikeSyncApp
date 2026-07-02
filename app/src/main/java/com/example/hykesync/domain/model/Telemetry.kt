package com.example.hykesync.domain.model

/**
 * Lecturas de los sensores del reloj.
 *
 * @property id Identificador único de la lectura.
 * @property sessionId ID de la sesión a la que pertenece esta lectura.
 * @property timestamp Timestamp de la lectura (ms).
 * @property heartRate Frecuencia cardíaca (bpm).
 * @property altitude Altitud (m).
 * @property pressure Presión atmosférica (hPa).
 * @property azimuth Orientación horizontal (grados).
 * @property pitch Orientación vertical (grados).
 * @property roll Inclinación lateral (grados).
 */
data class Telemetry(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val heartRate: Float,
    val altitude: Float,
    val pressure: Float,
    val azimuth: Float,
    val pitch: Float,
    val roll: Float,
    val connectionStatus: String = "Desconectado",
)
