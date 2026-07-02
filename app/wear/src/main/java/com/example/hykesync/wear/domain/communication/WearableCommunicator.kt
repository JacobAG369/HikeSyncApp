package com.example.hykesync.wear.domain.communication

import com.example.hykesync.domain.model.Telemetry

/**
 * Interfaz para la comunicación desde el reloj hacia el teléfono.
 */
interface WearableCommunicator {
    /**
     * Envía un lote de lecturas de telemetría al teléfono.
     */
    suspend fun sendTelemetryBatch(telemetryList: List<Telemetry>): Boolean

    /**
     * Envía un comando de control de sesión (ej. "SESSION_STOPPED").
     */
    suspend fun sendSessionCommand(path: String, sessionId: Long): Boolean
}
