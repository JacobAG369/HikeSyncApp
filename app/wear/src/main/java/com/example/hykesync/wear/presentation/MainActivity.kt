package com.example.hykesync.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hykesync.wear.presentation.theme.HikeSyncWearTheme
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sensorDataManager: SensorDataManager
    private var bodySensorPermissionState by mutableStateOf(BodySensorPermissionState.Unknown)
    private var hasRequestedBodySensorsPermission = false

    private var mobileNodes: Set<Node> = emptySet()
    private val capabilityName = "phone_app"

    private val bodySensorPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        bodySensorPermissionState = if (isGranted) {
            BodySensorPermissionState.Granted
        } else {
            BodySensorPermissionState.Denied
        }

        startSensorCapture(hasBodySensorsPermission = isGranted)

        if (!isGranted) {
            Log.e(TAG, "Permiso BODY_SENSORS denegado.")
            Toast.makeText(this, "Sin permiso cardiaco: se mostraran altitud y rumbo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorDataManager = SensorDataManager(this)

        setContent {
            HikeSyncWearTheme {
                val sensorState by sensorDataManager.sensorState.collectAsState()
                WearDashboardScreen(
                    sensorState = sensorState,
                    permissionState = bodySensorPermissionState,
                )
            }
        }

        findMobileNodes()
        checkPermissions(requestIfNeeded = true)
    }

    override fun onResume() {
        super.onResume()
        checkPermissions(requestIfNeeded = false)
    }

    private fun findMobileNodes() {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val capabilityInfo = Tasks.await(
                    Wearable.getCapabilityClient(this@MainActivity)
                        .getCapability(capabilityName, CapabilityClient.FILTER_REACHABLE)
                )
                mobileNodes = if (capabilityInfo.nodes.isNotEmpty()) {
                    capabilityInfo.nodes
                } else {
                    val connectedNodes = Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes)
                    connectedNodes.toSet()
                }
                Log.d(TAG, "Celulares encontrados con la app: ${mobileNodes.size}")
                for (node in mobileNodes) {
                    Log.d(TAG, "Nodo movil: ${node.displayName} (${node.id})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error buscando nodos por capacidad", e)
            }
        }
    }

    private fun checkPermissions(requestIfNeeded: Boolean) {
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) ==
            PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            Log.d(TAG, "Permiso BODY_SENSORS ya concedido.")
            bodySensorPermissionState = BodySensorPermissionState.Granted
            startSensorCapture(hasBodySensorsPermission = true)
            return
        }

        bodySensorPermissionState = BodySensorPermissionState.Denied
        startSensorCapture(hasBodySensorsPermission = false)

        if (requestIfNeeded && !hasRequestedBodySensorsPermission) {
            hasRequestedBodySensorsPermission = true
            Log.d(TAG, "Permiso BODY_SENSORS no concedido. Solicitando...")
            bodySensorPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        } else {
            Log.w(TAG, "Permiso BODY_SENSORS no disponible. Se continua sin frecuencia cardiaca.")
        }
    }

    private fun startSensorCapture(hasBodySensorsPermission: Boolean) {
        sensorDataManager.start(hasBodySensorsPermission = hasBodySensorsPermission)
    }

    override fun onPause() {
        super.onPause()
        sensorDataManager.stop()
        Log.d(TAG, "Captura de sensores detenida (onPause).")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

enum class BodySensorPermissionState {
    Unknown,
    Granted,
    Denied,
}
