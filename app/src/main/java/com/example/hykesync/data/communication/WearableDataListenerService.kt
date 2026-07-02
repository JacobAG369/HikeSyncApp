package com.example.hykesync.data.communication

import android.util.Log
import com.example.hykesync.data.database.HikeSyncDatabase
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.data.repository.TelemetryRepositoryImpl
import com.example.hykesync.domain.model.Telemetry
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearableDataListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var telemetryRepository: TelemetryRepositoryImpl
    private lateinit var sessionRepository: SessionRepositoryImpl

    override fun onCreate() {
        super.onCreate()
        val database = HikeSyncDatabase.getDatabase(this)
        telemetryRepository = TelemetryRepositoryImpl(database.telemetryDao())
        sessionRepository = SessionRepositoryImpl(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) {
                return@forEach
            }

            when (event.dataItem.uri.path) {
                "/telemetry_batch" -> handleTelemetryBatch(DataMapItem.fromDataItem(event.dataItem).dataMap)
                "/telemetry_data" -> handleTelemetryData(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        }
    }

    private fun handleTelemetryBatch(dataMap: com.google.android.gms.wearable.DataMap) {
        val activeSessionId = sessionRepository.getActiveSessionId()
        if (activeSessionId == null) {
            Log.w("WearableListener", "Batch descartado: no hay sesion activa")
            return
        }

        val batchList = dataMap.getDataMapArrayList("batch") ?: return
        val telemetryBatch = batchList.map { dm ->
            Telemetry(
                sessionId = activeSessionId,
                timestamp = dm.getLong("timestamp"),
                heartRate = dm.getFloat("heartRate"),
                altitude = dm.getFloat("altitude"),
                pressure = dm.getFloat("pressure"),
                azimuth = dm.getFloat("azimuth"),
                pitch = dm.getFloat("pitch"),
                roll = dm.getFloat("roll"),
                connectionStatus = dm.getString("connectionStatus") ?: "Desconectado"
            )
        }

        scope.launch {
            telemetryRepository.saveTelemetryBatch(telemetryBatch)
            Log.d("WearableListener", "Saved batch of ${telemetryBatch.size} items")
        }
    }

    private fun handleTelemetryData(dataMap: com.google.android.gms.wearable.DataMap) {
        val activeSessionId = sessionRepository.getActiveSessionId()
        if (activeSessionId == null) {
            Log.w("WearableListener", "Telemetria descartada: no hay sesion activa")
            return
        }

        val telemetry = Telemetry(
            sessionId = activeSessionId,
            timestamp = dataMap.getLong("timestamp"),
            heartRate = readOptionalFloat(dataMap, "hasHeartRate", "heartRate"),
            altitude = readOptionalFloat(dataMap, "hasAltitude", "altitude"),
            pressure = readOptionalFloat(dataMap, "hasPressure", "pressure"),
            azimuth = readOptionalFloat(dataMap, "hasAzimuth", "azimuth"),
            pitch = readOptionalFloat(dataMap, "hasPitch", "pitch"),
            roll = readOptionalFloat(dataMap, "hasRoll", "roll"),
            connectionStatus = dataMap.getString("connectionStatus") ?: "Desconectado"
        )

        scope.launch {
            telemetryRepository.saveTelemetry(telemetry)
            Log.d("WearableListener", "Telemetria guardada para sesion $activeSessionId")
        }
    }

    private fun readOptionalFloat(
        dataMap: com.google.android.gms.wearable.DataMap,
        hasKey: String,
        valueKey: String
    ): Float {
        return if (dataMap.getBoolean(hasKey, false)) dataMap.getFloat(valueKey) else 0f
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val sessionIdString = String(messageEvent.data)
        val sessionId = sessionIdString.toLongOrNull() ?: return

        Log.d("WearableListener", "Message received: $path for session: $sessionId")

        when (path) {
            "/start_session" -> {
                scope.launch {
                    sessionRepository.startNewSession(sessionId)
                }
            }
            "/stop_session" -> {
                scope.launch {
                    sessionRepository.stopActiveSession()
                }
            }
        }
    }
}
