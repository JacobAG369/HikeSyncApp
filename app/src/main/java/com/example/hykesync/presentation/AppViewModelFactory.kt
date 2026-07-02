package com.example.hykesync.presentation

import com.example.hykesync.data.database.dao.LocationPointDao
import com.example.hykesync.data.database.dao.SessionDao
import com.example.hykesync.data.database.dao.TelemetryDao
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.data.repository.SyncManager
import com.example.hykesync.domain.communication.AppCommunicator
import com.example.hykesync.domain.location.LocationTracker
import com.example.hykesync.domain.repository.TelemetryRepository

class AppViewModelFactory(
    private val telemetryRepository: TelemetryRepository,
    private val locationTracker: LocationTracker,
    private val locationPointDao: LocationPointDao,
    private val telemetryDao: TelemetryDao,
    private val sessionDao: SessionDao,
    private val communicator: AppCommunicator,
    private val sessionRepository: SessionRepositoryImpl,
    private val syncManager: SyncManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(
                telemetryRepository,
                locationTracker,
                locationPointDao,
                telemetryDao,
                sessionDao,
                communicator,
                sessionRepository,
                syncManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
