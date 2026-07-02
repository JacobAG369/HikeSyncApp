package com.example.hykesync.wear.data.sensors

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DeltaDataType
import com.example.hykesync.wear.domain.sensors.HeartRateSensor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Implementación de [HeartRateSensor] usando Health Services MeasureClient.
 */
class HeartRateSensorManager(context: Context) : HeartRateSensor {
    private val measureClient = HealthServices.getClient(context).measureClient

    override fun getHeartRateFlow(): Flow<Float> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                // Manejar cambios en la disponibilidad si es necesario
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateSamples = data.getData(DataType.HEART_RATE_BPM)
                if (heartRateSamples.isNotEmpty()) {
                    val lastSample = heartRateSamples.last()
                    trySend(lastSample.value.toFloat())
                }
            }
        }

        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
        }
    }
}
