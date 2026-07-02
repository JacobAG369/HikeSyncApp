package com.example.hykesync.domain.model

/**
 * Puntos GPS registrados por el teléfono.
 *
 * @property id Identificador único del punto.
 * @property sessionId ID de la sesión a la que pertenece este punto.
 * @property timestamp Timestamp de la lectura (ms).
 * @property latitude Latitud.
 * @property longitude Longitud.
 */
data class LocationPoint(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double
)
