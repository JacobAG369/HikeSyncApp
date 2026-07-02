package com.example.hykesync.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hykesync.wear.domain.communication.WearableCommunicator
import com.example.hykesync.wear.domain.sensors.EnvironmentSensor
import com.example.hykesync.wear.domain.sensors.HeartRateSensor

class WearViewModelFactory(
    private val heartRateSensor: HeartRateSensor,
    private val environmentSensor: EnvironmentSensor,
    private val communicator: WearableCommunicator
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WearViewModel::class.java)) {
            return WearViewModel(heartRateSensor, environmentSensor, communicator) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
