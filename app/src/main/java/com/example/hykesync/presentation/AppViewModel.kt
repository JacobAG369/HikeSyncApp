package com.example.hykesync.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hykesync.data.database.dao.LocationPointDao
import com.example.hykesync.data.database.dao.SessionDao
import com.example.hykesync.data.database.dao.TelemetryDao
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.data.repository.SyncManager
import com.example.hykesync.domain.communication.AppCommunicator
import com.example.hykesync.domain.location.LocationTracker
import com.example.hykesync.domain.repository.TelemetryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ViewModel que consolida los datos del reloj (Room) y el GPS local.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val telemetryRepository: TelemetryRepository,
    private val locationTracker: LocationTracker,
    private val locationPointDao: LocationPointDao,
    private val telemetryDao: TelemetryDao,
    private val sessionDao: SessionDao,
    private val communicator: AppCommunicator,
    private val sessionRepository: SessionRepositoryImpl,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private var locationTrackingJob: Job? = null

    init {
        restoreActiveSession()
        observeTelemetry()
        observeSavedLocation()
        observeRouteHistory()
        refreshLinkedDeviceState()
    }

    private fun restoreActiveSession() {
        val activeSessionId = sessionRepository.getActiveSessionId() ?: return
        _uiState.update {
            it.copy(
                isSessionActive = true,
                sessionId = activeSessionId,
                connectionStatus = "Sesion activa"
            )
        }
    }

    fun refreshLinkedDeviceState() {
        viewModelScope.launch {
            val nodeName = communicator.getReachableWearNodeName()
            _uiState.update { state ->
                state.copy(
                    linkedDeviceName = nodeName ?: "Sin dispositivo vinculado",
                    isWearDeviceReachable = nodeName != null,
                    connectionStatus = when {
                        state.isSessionActive && nodeName != null -> "Reloj sincronizado"
                        state.isSessionActive -> "Sesion activa"
                        nodeName != null -> "Conectado"
                        else -> "Desconectado"
                    },
                )
            }
        }
    }

    fun startLocationTracking() {
        if (locationTrackingJob != null) return

        locationTrackingJob = locationTracker.getLocationFlow(5000L)
            .onEach { location ->
                _uiState.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }
            .catch {
                _uiState.update { state ->
                    state.copy(error = "No se pudo iniciar el GPS del celular")
                }
                locationTrackingJob = null
            }
            .launchIn(viewModelScope)
    }

    private fun observeSavedLocation() {
        _uiState.map { it.sessionId }
            .flatMapLatest { sessionId ->
                if (sessionId > 0L) {
                    locationPointDao.getLatestLocationPointForSession(sessionId)
                } else {
                    flowOf(null)
                }
            }
            .onEach { locationPoint ->
                locationPoint?.let { point ->
                    _uiState.update {
                        it.copy(
                            latitude = point.latitude,
                            longitude = point.longitude
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeRouteHistory() {
        sessionDao.getAllSessions()
            .onEach { sessions ->
                val history = sessions.map { session ->
                    val lastTelemetry = telemetryDao.getLatestTelemetrySnapshotForSession(session.id)
                    val lastLocation = locationPointDao.getLatestLocationPointSnapshotForSession(session.id)
                    RouteHistoryItem(
                        sessionId = session.id,
                        startTime = session.startTime,
                        endTime = session.endTime,
                        status = session.status,
                        telemetryCount = telemetryDao.getTelemetryCountForSession(session.id),
                        locationCount = locationPointDao.getLocationCountForSession(session.id),
                        lastHeartRate = lastTelemetry?.heartRate,
                        lastAltitude = lastTelemetry?.altitude,
                        lastLatitude = lastLocation?.latitude,
                        lastLongitude = lastLocation?.longitude,
                    )
                }
                _uiState.update { it.copy(routeHistory = history) }
            }
            .catch {
                _uiState.update { it.copy(routeHistory = emptyList()) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTelemetry() {
        // Observamos la última telemetría de la sesión activa si existe
        _uiState.map { it.sessionId }
            .flatMapLatest { sessionId ->
                telemetryRepository.getTelemetryForSession(sessionId)
            }
            .catch { emit(emptyList()) }
            .onEach { telemetryList ->
                telemetryList.lastOrNull()?.let { last ->
                    _uiState.update {
                        it.copy(
                            lastHeartRate = last.heartRate,
                            lastAltitude = last.altitude,
                            lastAzimuth = last.azimuth,
                            latestTelemetryConnectionStatus = last.connectionStatus,
                            connectionStatus = last.connectionStatus,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleSession() {
        if (_uiState.value.isSessionActive) {
            stopSession()
        } else {
            startSession()
        }
    }

    private fun startSession() {
        val newSessionId = System.currentTimeMillis()
        viewModelScope.launch {
            val success = communicator.sendSessionCommand("/start_session", newSessionId)
            if (success) {
                _uiState.update { it.copy(
                    isSessionActive = true,
                    sessionId = newSessionId,
                    connectionStatus = "Reloj sincronizado"
                ) }
                refreshLinkedDeviceState()
            } else {
                _uiState.update { it.copy(error = "No se pudo contactar con el reloj") }
            }
        }
    }

    private fun stopSession() {
        val currentSessionId = _uiState.value.sessionId
        viewModelScope.launch {
            val success = communicator.sendSessionCommand("/stop_session", currentSessionId)
            if (success) {
                _uiState.update {
                    it.copy(
                        isSessionActive = false,
                        sessionId = 0L,
                        connectionStatus = "Ruta finalizada",
                        latestTelemetryConnectionStatus = "Sin telemetria reciente",
                    )
                }
                refreshLinkedDeviceState()
            } else {
                _uiState.update { it.copy(error = "Error al detener sesión en el reloj") }
            }
        }
    }

    suspend fun syncActiveSession(): SyncManager.SyncResult {
        val sessionId = _uiState.value.sessionId
        if (sessionId <= 0L) {
            return SyncManager.SyncResult.Error
        }
        _isSyncing.value = true
        return try {
            syncManager.syncSessionToBackend(sessionId.toString())
        } finally {
            _isSyncing.value = false
        }
    }

    fun loadRouteDetail(sessionId: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRouteDetailLoading = true,
                    routeDetailError = null,
                    selectedRouteDetail = if (it.selectedRouteDetail?.sessionId == sessionId) it.selectedRouteDetail else null,
                )
            }

            val session = sessionDao.getSessionById(sessionId)
            if (session == null) {
                _uiState.update {
                    it.copy(
                        isRouteDetailLoading = false,
                        routeDetailError = "No se encontro la ruta seleccionada",
                        selectedRouteDetail = null,
                    )
                }
                return@launch
            }

            val telemetry = telemetryDao.getTelemetryListForSession(sessionId)
            val locations = locationPointDao.getLocationPointListForSession(sessionId)

            val validHeartRates = telemetry.map { it.heartRate }.filter { it.isFinite() && it > 0f }
            val validAltitudes = telemetry.map { it.altitude }.filter { it.isFinite() && it > 0f }
            val routePoints = locations.map { point -> RouteMapPoint(point.latitude, point.longitude) }
            val durationMillis = (session.endTime ?: System.currentTimeMillis()) - session.startTime
            val distanceKm = calculateDistanceKm(routePoints)

            _uiState.update {
                it.copy(
                    isRouteDetailLoading = false,
                    routeDetailError = null,
                    selectedRouteDetail = RouteDetailUiState(
                        sessionId = session.id,
                        startTime = session.startTime,
                        endTime = session.endTime,
                        status = session.status,
                        durationMillis = durationMillis,
                        telemetryCount = telemetry.size,
                        locationCount = locations.size,
                        averageHeartRate = validHeartRates.takeIf { values -> values.isNotEmpty() }
                            ?.average()
                            ?.toFloat(),
                        maxHeartRate = validHeartRates.maxOrNull(),
                        minAltitude = validAltitudes.minOrNull(),
                        maxAltitude = validAltitudes.maxOrNull(),
                        distanceKm = distanceKm,
                        elevationGainMeters = calculateElevationGain(validAltitudes),
                        elevationLossMeters = calculateElevationLoss(validAltitudes),
                        averageSpeedKmh = calculateAverageSpeedKmh(
                            distanceKm = distanceKm,
                            durationMillis = durationMillis,
                        ),
                        heartRateSeries = validHeartRates.takeLast(40),
                        altitudeSeries = validAltitudes.takeLast(40),
                        routePoints = routePoints,
                    )
                )
            }
        }
    }

    fun clearRouteDetail() {
        _uiState.update {
            it.copy(
                selectedRouteDetail = null,
                isRouteDetailLoading = false,
                routeDetailError = null,
            )
        }
    }
}

private fun calculateDistanceKm(points: List<RouteMapPoint>): Float {
    if (points.size < 2) return 0f
    var distanceMeters = 0.0
    points.zipWithNext().forEach { (start, end) ->
        distanceMeters += haversineMeters(start.latitude, start.longitude, end.latitude, end.longitude)
    }
    return (distanceMeters / 1000.0).toFloat()
}

private fun calculateElevationGain(altitudes: List<Float>): Float {
    if (altitudes.size < 2) return 0f
    var gain = 0f
    altitudes.zipWithNext().forEach { (start, end) ->
        val diff = end - start
        if (diff > 0f) gain += diff
    }
    return gain
}

private fun calculateElevationLoss(altitudes: List<Float>): Float {
    if (altitudes.size < 2) return 0f
    var loss = 0f
    altitudes.zipWithNext().forEach { (start, end) ->
        val diff = end - start
        if (diff < 0f) loss += -diff
    }
    return loss
}

private fun calculateAverageSpeedKmh(distanceKm: Float, durationMillis: Long): Float? {
    if (distanceKm <= 0f || durationMillis <= 0L) return null
    val hours = durationMillis / 3_600_000f
    if (hours <= 0f) return null
    return distanceKm / hours
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val originLat = Math.toRadians(lat1)
    val destinationLat = Math.toRadians(lat2)
    val a = sin(dLat / 2).pow(2) + cos(originLat) * cos(destinationLat) * sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return earthRadiusMeters * c
}
