package com.example.hykesync.domain.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz para el rastreo de ubicación mediante GPS.
 */
interface LocationTracker {
    /**
     * Emite actualizaciones regulares de la ubicación del usuario.
     */
    fun getLocationFlow(intervalMillis: Long): Flow<Location>
}
