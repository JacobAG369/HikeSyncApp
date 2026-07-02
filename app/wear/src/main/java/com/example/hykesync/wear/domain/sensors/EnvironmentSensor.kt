package com.example.hykesync.wear.domain.sensors

import com.example.hykesync.wear.domain.model.EnvironmentData
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz para la lectura de sensores de entorno (Barómetro y Orientación).
 */
interface EnvironmentSensor {
    /**
     * Emite datos actualizados de presión, altitud y orientación.
     */
    fun getEnvironmentFlow(): Flow<EnvironmentData>
}
