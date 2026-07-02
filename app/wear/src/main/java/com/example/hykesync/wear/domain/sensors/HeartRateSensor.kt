package com.example.hykesync.wear.domain.sensors

import kotlinx.coroutines.flow.Flow

/**
 * Interfaz para la lectura de la frecuencia cardíaca desde Health Services.
 */
interface HeartRateSensor {
    /**
     * Emite la frecuencia cardíaca actual en pulsaciones por minuto (bpm).
     */
    fun getHeartRateFlow(): Flow<Float>
}
