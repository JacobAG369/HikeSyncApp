package com.example.hykesync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.hykesync.data.communication.AppCommunicatorImpl
import com.example.hykesync.data.database.HikeSyncDatabase
import com.example.hykesync.data.location.FusedLocationTracker
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.data.repository.SyncManager
import com.example.hykesync.data.repository.TelemetryRepositoryImpl
import com.example.hykesync.presentation.HikeSyncNavHost
import com.example.hykesync.presentation.AppViewModel
import com.example.hykesync.presentation.AppViewModelFactory
import com.example.hykesync.ui.theme.HykeSyncTheme

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }

    private val viewModel: AppViewModel by viewModels {
        val database = HikeSyncDatabase.getDatabase(applicationContext)
        AppViewModelFactory(
            telemetryRepository = TelemetryRepositoryImpl(database.telemetryDao()),
            locationTracker = FusedLocationTracker(applicationContext),
            locationPointDao = database.locationPointDao(),
            telemetryDao = database.telemetryDao(),
            sessionDao = database.sessionDao(),
            communicator = AppCommunicatorImpl(applicationContext),
            sessionRepository = SessionRepositoryImpl(applicationContext),
            syncManager = SyncManager(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureLocationPermission()
        setContent {
            HykeSyncTheme {
                HikeSyncNavHost(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ensureLocationPermission()
        viewModel.refreshLinkedDeviceState()
    }

    private fun ensureLocationPermission() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            viewModel.startLocationTracking()
            return
        }

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }
}
