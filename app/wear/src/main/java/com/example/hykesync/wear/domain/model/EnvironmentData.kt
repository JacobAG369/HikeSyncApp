package com.example.hykesync.wear.domain.model

/**
 * Datos del entorno capturados por los sensores del reloj.
 *
 * @property pressure Presión atmosférica (hPa).
 * @property altitude Altitud estimada (m).
 * @property azimuth Orientación horizontal (grados).
 * @property pitch Orientación vertical (grados).
 * @property roll Inclinación lateral (grados).
 */
data class EnvironmentData(
    val pressure: Float = 0f,
    val altitude: Float = 0f,
    val azimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
)
