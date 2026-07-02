package com.example.hykesync.domain.repository

import com.example.hykesync.domain.model.Telemetry
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para la gestión de datos de telemetría.
 */
interface TelemetryRepository {
    suspend fun saveTelemetry(telemetry: Telemetry)
    suspend fun saveTelemetryBatch(telemetryList: List<Telemetry>)
    fun getTelemetryForSession(sessionId: Long): Flow<List<Telemetry>>
}
