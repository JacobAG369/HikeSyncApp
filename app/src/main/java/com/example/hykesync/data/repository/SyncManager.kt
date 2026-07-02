package com.example.hykesync.data.repository

import android.content.Context
import android.util.Log
import com.example.hykesync.data.database.HikeSyncDatabase
import com.example.hykesync.network.HikeSyncApi
import com.example.hykesync.network.RetrofitProvider
import com.example.hykesync.network.dto.LocationDto
import com.example.hykesync.network.dto.SyncRequest
import com.example.hykesync.network.dto.TelemetryDto

class SyncManager(
    context: Context,
    private val api: HikeSyncApi = RetrofitProvider.createApi()
) {
    private val database = HikeSyncDatabase.getDatabase(context.applicationContext)
    private val telemetryDao = database.telemetryDao()
    private val locationPointDao = database.locationPointDao()

    suspend fun syncSessionToBackend(sessionId: String): SyncResult {
        val localSessionId = sessionId.toLongOrNull()
        if (localSessionId == null) {
            Log.e(TAG, "sessionId invalido para sincronizacion: $sessionId")
            return SyncResult.Error
        }

        return try {
            val telemetry = telemetryDao.getUnsyncedTelemetryForSession(localSessionId)
                .map { entity ->
                    TelemetryDto(
                        timestamp = entity.timestamp,
                        heartRate = entity.heartRate,
                        altitude = entity.altitude,
                        pressure = entity.pressure
                    )
                }

            val location = locationPointDao.getUnsyncedLocationPointsForSession(localSessionId)
                .map { entity ->
                    LocationDto(
                        timestamp = entity.timestamp,
                        latitud = entity.latitude,
                        longitud = entity.longitude
                    )
                }

            if (telemetry.isEmpty() && location.isEmpty()) {
                Log.i(TAG, "No hay datos nuevos para sincronizar en sesion $sessionId")
                return SyncResult.NoNewData
            }

            val request = SyncRequest(
                sessionId = sessionId,
                telemetry = telemetry,
                location = location
            )

            val response = api.syncData(request)
            if (response.isSuccessful) {
                telemetryDao.markSessionTelemetryAsSynced(localSessionId)
                locationPointDao.markSessionLocationPointsAsSynced(localSessionId)
                Log.i(TAG, "Sincronizacion exitosa para sesion $sessionId con HTTP ${response.code()}")
                SyncResult.Success
            } else {
                Log.e(TAG, "Fallo de sincronizacion HTTP ${response.code()} para sesion $sessionId")
                SyncResult.Error
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error sincronizando sesion $sessionId", exception)
            SyncResult.Error
        }
    }

    enum class SyncResult {
        Success,
        NoNewData,
        Error
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
