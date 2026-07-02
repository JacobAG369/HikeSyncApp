package com.example.hykesync.wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.hykesync.domain.model.Telemetry
import com.example.hykesync.wear.data.cache.WearTelemetryCacheStore
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SensorDataManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val dataClient = Wearable.getDataClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val cacheStore = WearTelemetryCacheStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val heartRateSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val pressureSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _sensorState = MutableStateFlow(WearSensorState())
    val sensorState: StateFlow<WearSensorState> = _sensorState.asStateFlow()

    private var latestHeartRate: Float? = null
    private var latestPressure: Float? = null
    private var latestAccelerationX: Float? = null
    private var latestAccelerationY: Float? = null
    private var latestAccelerationZ: Float? = null
    private var latestAltitude: Float? = null
    private var latestAzimuth: Float? = null
    private var latestPitch: Float? = null
    private var latestRoll: Float? = null
    private var latestGravity: FloatArray? = null
    private var latestGeomagnetic: FloatArray? = null
    private var lastTelemetryTimestamp = 0L
    private var lastSentHeartRate: Float? = null
    private var lastSentPressure: Float? = null
    private var lastSentAltitude: Float? = null
    private var lastSentAccelerationX: Float? = null
    private var lastSentAccelerationY: Float? = null
    private var lastSentAccelerationZ: Float? = null
    private var isStarted = false
    private var bodySensorsPermissionGranted = false

    fun start(hasBodySensorsPermission: Boolean) {
        if (isStarted && bodySensorsPermissionGranted == hasBodySensorsPermission) return

        if (isStarted) {
            stop()
        }

        if (sensorManager == null) {
            Log.e(TAG, "SensorManager no disponible")
            return
        }

        bodySensorsPermissionGranted = hasBodySensorsPermission

        if (hasBodySensorsPermission) {
            registerSensor(heartRateSensor, "frecuencia cardiaca")
        } else {
            latestHeartRate = null
        }

        registerSensor(pressureSensor, "presion")
        registerSensor(accelerometerSensor, "acelerometro")
        registerSensor(magnetometerSensor, "magnetometro")
        isStarted = true
        emitSensorState()
        scope.launch {
            flushPendingTelemetry(isPhoneReachable = isPhoneReachable())
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        isStarted = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                latestHeartRate = event.values.firstOrNull()
            }

            Sensor.TYPE_PRESSURE -> {
                latestPressure = event.values.firstOrNull()
                latestAltitude = latestPressure?.let {
                    SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, it)
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                latestAccelerationX = event.values.getOrNull(0)
                latestAccelerationY = event.values.getOrNull(1)
                latestAccelerationZ = event.values.getOrNull(2)
                latestGravity = event.values.clone()
                updateOrientation()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                latestGeomagnetic = event.values.clone()
                updateOrientation()
            }

            else -> return
        }

        emitSensorState()
        sendLatestTelemetryIfNeeded()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun registerSensor(sensor: Sensor?, sensorName: String) {
        if (sensor == null) {
            Log.w(TAG, "Sensor no disponible: $sensorName")
            return
        }

        try {
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (exception: Exception) {
            Log.e(TAG, "No se pudo registrar el sensor $sensorName", exception)
        }
    }

    private fun updateOrientation() {
        val gravity = latestGravity ?: return
        val geomagnetic = latestGeomagnetic ?: return
        val rotationMatrix = FloatArray(9)
        val inclinationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            latestAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat().normalizeDegrees()
            latestPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            latestRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        }
    }

    private fun emitSensorState() {
        _sensorState.value = WearSensorState(
            heartRate = latestHeartRate,
            pressure = latestPressure,
            altitude = latestAltitude,
            azimuth = latestAzimuth,
            pitch = latestPitch,
            roll = latestRoll,
            hasHeartRateSensor = heartRateSensor != null,
            hasPressureSensor = pressureSensor != null,
            hasOrientationSensors = accelerometerSensor != null && magnetometerSensor != null,
            hasBodySensorPermission = bodySensorsPermissionGranted,
            pendingSyncCount = cacheStore.pendingCount(),
            maxPendingSyncCount = cacheStore.maxPendingItems(),
            isCacheNearCapacity = cacheStore.isNearCapacity(),
        )
    }

    private fun sendLatestTelemetryIfNeeded() {
        if (!shouldSendTelemetry()) {
            return
        }

        scope.launch {
            val phoneReachable = isPhoneReachable()
            flushPendingTelemetry(isPhoneReachable = phoneReachable)

            val telemetry = buildTelemetry(
                timestamp = System.currentTimeMillis(),
                isPhoneReachable = phoneReachable,
            )

            if (!phoneReachable) {
                cacheTelemetry(telemetry)
                return@launch
            }

            val request = PutDataMapRequest.create(TELEMETRY_PATH).apply {
                dataMap.putLong(KEY_TIMESTAMP, telemetry.timestamp)
                dataMap.putBoolean(KEY_HAS_HEART_RATE, latestHeartRate != null)
                dataMap.putFloat(KEY_HEART_RATE, latestHeartRate ?: DEFAULT_FLOAT)
                dataMap.putBoolean(KEY_HAS_PRESSURE, latestPressure != null)
                dataMap.putFloat(KEY_PRESSURE, latestPressure ?: DEFAULT_FLOAT)
                dataMap.putBoolean(KEY_HAS_ALTITUDE, latestAltitude != null)
                dataMap.putFloat(KEY_ALTITUDE, latestAltitude ?: DEFAULT_FLOAT)
                dataMap.putBoolean(KEY_HAS_AZIMUTH, latestAzimuth != null)
                dataMap.putFloat(KEY_AZIMUTH, latestAzimuth ?: DEFAULT_FLOAT)
                dataMap.putBoolean(KEY_HAS_PITCH, latestPitch != null)
                dataMap.putFloat(KEY_PITCH, latestPitch ?: DEFAULT_FLOAT)
                dataMap.putBoolean(KEY_HAS_ROLL, latestRoll != null)
                dataMap.putFloat(KEY_ROLL, latestRoll ?: DEFAULT_FLOAT)
                dataMap.putString(KEY_CONNECTION_STATUS, telemetry.connectionStatus)
            }

            try {
                dataClient.putDataItem(request.asPutDataRequest()).await()
                markTelemetryRecorded(telemetry)
            } catch (exception: Exception) {
                Log.e(TAG, "Error enviando telemetria", exception)
                cacheTelemetry(telemetry)
            }
        }
    }

    private fun buildTelemetry(timestamp: Long, isPhoneReachable: Boolean): Telemetry {
        return Telemetry(
            sessionId = 0L,
            timestamp = timestamp,
            heartRate = latestHeartRate ?: DEFAULT_FLOAT,
            altitude = latestAltitude ?: DEFAULT_FLOAT,
            pressure = latestPressure ?: DEFAULT_FLOAT,
            azimuth = latestAzimuth ?: DEFAULT_FLOAT,
            pitch = latestPitch ?: DEFAULT_FLOAT,
            roll = latestRoll ?: DEFAULT_FLOAT,
            connectionStatus = if (isPhoneReachable) CONNECTION_CONNECTED else CONNECTION_DISCONNECTED,
        )
    }

    private suspend fun flushPendingTelemetry(isPhoneReachable: Boolean) {
        val pendingTelemetry = cacheStore.getPendingTelemetry()
        if (pendingTelemetry.isEmpty()) {
            return
        }

        if (!isPhoneReachable) {
            Log.d(TAG, "Telefono no disponible. Pendientes en cache: ${cacheStore.pendingCount()}")
            return
        }

        val batchRequest = PutDataMapRequest.create(TELEMETRY_BATCH_PATH).apply {
            val batch = ArrayList<DataMap>(pendingTelemetry.size)
            pendingTelemetry.forEach { telemetry ->
                batch.add(
                    DataMap().apply {
                        putLong(KEY_TIMESTAMP, telemetry.timestamp)
                        putFloat(KEY_HEART_RATE, telemetry.heartRate)
                        putFloat(KEY_ALTITUDE, telemetry.altitude)
                        putFloat(KEY_PRESSURE, telemetry.pressure)
                        putFloat(KEY_AZIMUTH, telemetry.azimuth)
                        putFloat(KEY_PITCH, telemetry.pitch)
                        putFloat(KEY_ROLL, telemetry.roll)
                        putString(KEY_CONNECTION_STATUS, telemetry.connectionStatus)
                    }
                )
            }
            dataMap.putDataMapArrayList(KEY_BATCH, batch)
            setUrgent()
        }

        try {
            dataClient.putDataItem(batchRequest.asPutDataRequest()).await()
            cacheStore.clearPendingTelemetry()
            emitSensorState()
            Log.d(TAG, "Telemetria offline sincronizada: ${pendingTelemetry.size} lecturas")
        } catch (exception: Exception) {
            Log.e(TAG, "No se pudo sincronizar la cache offline", exception)
        }
    }

    private suspend fun isPhoneReachable(): Boolean {
        return try {
            val capabilityNodes = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes

            if (capabilityNodes.isNotEmpty()) {
                true
            } else {
                val connectedNodes = nodeClient.connectedNodes.await()
                if (connectedNodes.isNotEmpty()) {
                    Log.w(TAG, "Capability '$PHONE_CAPABILITY' sin nodos; usando NodeClient fallback")
                }
                connectedNodes.isNotEmpty()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "No se pudo verificar la conexion con el telefono", exception)
            false
        }
    }

    private fun cacheTelemetry(telemetry: Telemetry) {
        cacheStore.saveTelemetry(telemetry)
        markTelemetryRecorded(telemetry)
        emitSensorState()
        Log.d(TAG, "Telemetria guardada en cache offline. Pendientes: ${cacheStore.pendingCount()}")
    }

    private fun markTelemetryRecorded(telemetry: Telemetry) {
        lastTelemetryTimestamp = telemetry.timestamp
        lastSentHeartRate = latestHeartRate
        lastSentPressure = latestPressure
        lastSentAltitude = latestAltitude
        lastSentAccelerationX = latestAccelerationX
        lastSentAccelerationY = latestAccelerationY
        lastSentAccelerationZ = latestAccelerationZ
    }

    private fun shouldSendTelemetry(): Boolean {
        val now = System.currentTimeMillis()
        if (lastTelemetryTimestamp == 0L) {
            return true
        }

        if (now - lastTelemetryTimestamp >= TELEMETRY_INTERVAL_MS) {
            return true
        }

        if (latestHeartRate.hasMeaningfulChange(lastSentHeartRate, HEART_RATE_DELTA)) {
            return true
        }

        if (latestAltitude.hasMeaningfulChange(lastSentAltitude, ALTITUDE_DELTA_METERS)) {
            return true
        }

        if (latestPressure.hasMeaningfulChange(lastSentPressure, PRESSURE_DELTA_HPA)) {
            return true
        }

        return latestAccelerationX.hasMeaningfulChange(lastSentAccelerationX, ACCELERATION_DELTA) ||
            latestAccelerationY.hasMeaningfulChange(lastSentAccelerationY, ACCELERATION_DELTA) ||
            latestAccelerationZ.hasMeaningfulChange(lastSentAccelerationZ, ACCELERATION_DELTA)
    }

    companion object {
        private const val TAG = "SensorDataManager"
        private const val PHONE_CAPABILITY = "phone_app"
        private const val TELEMETRY_PATH = "/telemetry_data"
        private const val TELEMETRY_BATCH_PATH = "/telemetry_batch"
        private const val DEFAULT_FLOAT = Float.NaN
        private const val TELEMETRY_INTERVAL_MS = 5_000L
        private const val HEART_RATE_DELTA = 2f
        private const val ALTITUDE_DELTA_METERS = 3f
        private const val PRESSURE_DELTA_HPA = 1f
        private const val ACCELERATION_DELTA = 1.5f
        private const val KEY_BATCH = "batch"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_HEART_RATE = "heartRate"
        private const val KEY_PRESSURE = "pressure"
        private const val KEY_ALTITUDE = "altitude"
        private const val KEY_AZIMUTH = "azimuth"
        private const val KEY_PITCH = "pitch"
        private const val KEY_ROLL = "roll"
        private const val KEY_CONNECTION_STATUS = "connectionStatus"
        private const val KEY_HAS_AZIMUTH = "hasAzimuth"
        private const val KEY_HAS_PITCH = "hasPitch"
        private const val KEY_HAS_ROLL = "hasRoll"
        private const val KEY_HAS_HEART_RATE = "hasHeartRate"
        private const val KEY_HAS_PRESSURE = "hasPressure"
        private const val KEY_HAS_ALTITUDE = "hasAltitude"
        private const val CONNECTION_CONNECTED = "Conectado"
        private const val CONNECTION_DISCONNECTED = "Desconectado"
    }
}

data class WearSensorState(
    val heartRate: Float? = null,
    val pressure: Float? = null,
    val altitude: Float? = null,
    val azimuth: Float? = null,
    val pitch: Float? = null,
    val roll: Float? = null,
    val hasHeartRateSensor: Boolean = false,
    val hasPressureSensor: Boolean = false,
    val hasOrientationSensors: Boolean = false,
    val hasBodySensorPermission: Boolean = false,
    val pendingSyncCount: Int = 0,
    val maxPendingSyncCount: Int = 0,
    val isCacheNearCapacity: Boolean = false,
)

private fun Float.normalizeDegrees(): Float {
    var normalized = this % 360f
    if (normalized < 0f) {
        normalized += 360f
    }
    return normalized
}

private fun Float?.hasMeaningfulChange(previous: Float?, threshold: Float): Boolean {
    if (this == null || previous == null) {
        return this != previous
    }

    return kotlin.math.abs(this - previous) >= threshold
}
