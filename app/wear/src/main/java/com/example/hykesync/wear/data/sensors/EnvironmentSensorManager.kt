package com.example.hykesync.wear.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.hykesync.wear.domain.model.EnvironmentData
import com.example.hykesync.wear.domain.sensors.EnvironmentSensor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlin.math.pow

/**
 * Implementación de [EnvironmentSensor] usando el SensorManager clásico.
 */
class EnvironmentSensorManager(context: Context) : EnvironmentSensor {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun getEnvironmentFlow(): Flow<EnvironmentData> {
        val pressureFlow = getPressureFlow()
        val orientationFlow = getOrientationFlow()

        return combine(pressureFlow, orientationFlow) { pressure, orientation ->
            EnvironmentData(
                pressure = pressure,
                altitude = calculateAltitude(pressure),
                azimuth = orientation[0],
                pitch = orientation[1],
                roll = orientation[2],
            )
        }
    }

    private fun getPressureFlow(): Flow<Float> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { trySend(it) }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: trySend(1013.25f) // Default sea-level pressure if no sensor

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun getOrientationFlow(): Flow<FloatArray> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var gravity: FloatArray? = null
        var geomagnetic: FloatArray? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity = event.values
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values
                }

                if ((gravity != null) && (geomagnetic != null)) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        // Convert from radians to degrees
                        val orientationDegrees = FloatArray(3) { Math.toDegrees(orientation[it].toDouble()).toFloat() }
                        trySend(orientationDegrees)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun calculateAltitude(pressure: Float): Float {
        // P0 is sea level pressure
        val p0 = 1013.25f
        return 44330 * (1 - (pressure / p0).pow(1 / 5.255f))
    }
}
