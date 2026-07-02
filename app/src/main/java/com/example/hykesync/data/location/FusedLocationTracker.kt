package com.example.hykesync.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.hykesync.data.database.HikeSyncDatabase
import com.example.hykesync.data.database.entities.LocationPointEntity
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.domain.location.LocationTracker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Implementación de [LocationTracker] usando FusedLocationProviderClient.
 */
class FusedLocationTracker(context: Context) : LocationTracker {
    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationPointDao = HikeSyncDatabase.getDatabase(appContext).locationPointDao()
    private val sessionRepository = SessionRepositoryImpl(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @SuppressLint("MissingPermission")
    override fun getLocationFlow(intervalMillis: Long): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    persistLocationIfSessionActive(location)
                    trySend(location)
                }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            client.removeLocationUpdates(callback)
        }
    }

    private fun persistLocationIfSessionActive(location: Location) {
        val activeSessionId = sessionRepository.getActiveSessionId() ?: return
        if (!location.isValidLocation()) return

        scope.launch {
            locationPointDao.insertLocationPoint(
                LocationPointEntity(
                    sessionId = activeSessionId,
                    timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    private fun Location.isValidLocation(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
