package com.example.hykesync.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hykesync.domain.model.Telemetry
import com.example.hykesync.wear.domain.communication.WearableCommunicator
import com.example.hykesync.wear.domain.sensors.EnvironmentSensor
import com.example.hykesync.wear.domain.sensors.HeartRateSensor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona el estado de la sesión y los sensores en el reloj.
 */
class WearViewModel(
    private val heartRateSensor: HeartRateSensor,
    private val environmentSensor: EnvironmentSensor,
    private val communicator: WearableCommunicator
) : ViewModel() {

    private val _uiState = MutableStateFlow(WearUiState())
    val uiState: StateFlow<WearUiState> = _uiState.asStateFlow()

    private var sensorJob: Job? = null

    init {
        // Observar sensores de forma continua para mostrar datos "glanceable"
        startSensorMonitoring()
    }

    private fun startSensorMonitoring() {
        combine(
            heartRateSensor.getHeartRateFlow(),
            environmentSensor.getEnvironmentFlow()
        ) { hr, env ->
            _uiState.update { it.copy(heartRate = hr, altitude = env.altitude) }
            
            // Si la sesión está activa, enviamos telemetría al teléfono
            if (_uiState.value.isSessionActive) {
                sendTelemetry(hr, env.altitude, env.pressure, env.azimuth, env.pitch, env.roll)
            }
        }.launchIn(viewModelScope)
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
                _uiState.update { it.copy(isSessionActive = true, sessionId = newSessionId) }
            } else {
                _uiState.update { it.copy(error = "Error al iniciar sesión") }
            }
        }
    }

    private fun stopSession() {
        val currentSessionId = _uiState.value.sessionId
        viewModelScope.launch {
            val success = communicator.sendSessionCommand("/stop_session", currentSessionId)
            if (success) {
                _uiState.update { it.copy(isSessionActive = false, sessionId = 0L) }
            } else {
                _uiState.update { it.copy(error = "Error al detener sesión") }
            }
        }
    }

    private fun sendTelemetry(hr: Float, alt: Float, press: Float, azi: Float, pit: Float, roll: Float) {
        val telemetry = Telemetry(
            sessionId = _uiState.value.sessionId,
            timestamp = System.currentTimeMillis(),
            heartRate = hr,
            altitude = alt,
            pressure = press,
            azimuth = azi,
            pitch = pit,
            roll = roll,
            connectionStatus = "Conectado",
        )
        viewModelScope.launch {
            communicator.sendTelemetryBatch(listOf(telemetry))
        }
    }
}
