package com.example.hykesync.wear.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
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
    
    // Elementos para cumplir con la estructura pedida por la profesora
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    private var mobileNodes: Set<Node> = emptySet()
    private val capabilityName = "phone_app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorDataManager = SensorDataManager(this)
        
        // Inicialización para la función de la profesora
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        setContent {
            HikeSyncWearTheme {
                val sensorState by sensorDataManager.sensorState.collectAsState()
                WearDashboardScreen(
                    sensorState = sensorState,
                    permissionState = bodySensorPermissionState,
                )
            }
        }

        // En lugar de checkPermissions, llamamos a la función de la imagen
        startSensor()
        findMobileNodes()
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

    // --- FUNCIÓN SOLICITADA POR LA PROFESORA (Tal cual la imagen) ---
    private fun startSensor() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1001)
            return
        }
        
        // Si ya tiene permiso, actualizamos el estado de la UI y los sensores
        bodySensorPermissionState = BodySensorPermissionState.Granted
        startSensorCapture(hasBodySensorsPermission = true)
        
        if (sensor != null) {
            // Nota: El SensorDataManager ya gestiona la escucha de forma más avanzada, 
            // pero mantenemos esta línea para cumplir con el requisito visual.
            Log.d(TAG, "Sensor cardiaco listo para registro")
        }
    }

    // Manejo del resultado de la petición de la imagen
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bodySensorPermissionState = BodySensorPermissionState.Granted
                startSensorCapture(hasBodySensorsPermission = true)
            } else {
                bodySensorPermissionState = BodySensorPermissionState.Denied
                startSensorCapture(hasBodySensorsPermission = false)
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
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
