package com.example.hykesync.wear.data.cache

import android.content.Context
import android.util.Log
import com.example.hykesync.domain.model.Telemetry
import org.json.JSONArray
import org.json.JSONObject

class WearTelemetryCacheStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTelemetry(telemetry: Telemetry) {
        val pendingItems = getPendingTelemetry().toMutableList()
        pendingItems.add(telemetry)
        persist(pendingItems.takeLast(MAX_PENDING_ITEMS))
    }

    fun getPendingTelemetry(): List<Telemetry> {
        return try {
            val raw = preferences.getString(KEY_PENDING_TELEMETRY, null) ?: return emptyList()
            val jsonArray = JSONArray(raw)
            buildList(jsonArray.length()) {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(
                        Telemetry(
                            sessionId = item.optLong(KEY_SESSION_ID),
                            timestamp = item.optLong(KEY_TIMESTAMP),
                            heartRate = item.optDouble(KEY_HEART_RATE).toFloat(),
                            altitude = item.optDouble(KEY_ALTITUDE).toFloat(),
                            pressure = item.optDouble(KEY_PRESSURE).toFloat(),
                            azimuth = item.optDouble(KEY_AZIMUTH).toFloat(),
                            pitch = item.optDouble(KEY_PITCH).toFloat(),
                            roll = item.optDouble(KEY_ROLL).toFloat(),
                            connectionStatus = item.optString(KEY_CONNECTION_STATUS, DEFAULT_CONNECTION_STATUS),
                        )
                    )
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Cache offline corrupta, se reinicia", exception)
            clearPendingTelemetry()
            emptyList()
        }
    }

    fun clearPendingTelemetry() {
        preferences.edit().remove(KEY_PENDING_TELEMETRY).apply()
    }

    fun pendingCount(): Int = getPendingTelemetry().size

    fun maxPendingItems(): Int = MAX_PENDING_ITEMS

    fun isNearCapacity(): Boolean = pendingCount() >= (MAX_PENDING_ITEMS * NEAR_CAPACITY_RATIO)

    private fun persist(items: List<Telemetry>) {
        val jsonArray = JSONArray()
        items.forEach { telemetry ->
            jsonArray.put(
                JSONObject()
                    .put(KEY_SESSION_ID, telemetry.sessionId)
                    .put(KEY_TIMESTAMP, telemetry.timestamp)
                    .put(KEY_HEART_RATE, telemetry.heartRate)
                    .put(KEY_ALTITUDE, telemetry.altitude)
                    .put(KEY_PRESSURE, telemetry.pressure)
                    .put(KEY_AZIMUTH, telemetry.azimuth)
                    .put(KEY_PITCH, telemetry.pitch)
                    .put(KEY_ROLL, telemetry.roll)
                    .put(KEY_CONNECTION_STATUS, telemetry.connectionStatus)
            )
        }

        preferences.edit().putString(KEY_PENDING_TELEMETRY, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "wear_telemetry_cache"
        private const val KEY_PENDING_TELEMETRY = "pending_telemetry"
        private const val KEY_SESSION_ID = "sessionId"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_HEART_RATE = "heartRate"
        private const val KEY_ALTITUDE = "altitude"
        private const val KEY_PRESSURE = "pressure"
        private const val KEY_AZIMUTH = "azimuth"
        private const val KEY_PITCH = "pitch"
        private const val KEY_ROLL = "roll"
        private const val KEY_CONNECTION_STATUS = "connectionStatus"
        private const val DEFAULT_CONNECTION_STATUS = "Desconectado"
        private const val MAX_PENDING_ITEMS = 250
        private const val NEAR_CAPACITY_RATIO = 0.8f
        private const val TAG = "WearTelemetryCache"
    }
}
