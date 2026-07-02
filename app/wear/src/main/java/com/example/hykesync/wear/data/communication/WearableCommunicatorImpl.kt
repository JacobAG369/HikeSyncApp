package com.example.hykesync.wear.data.communication

import android.content.Context
import com.example.hykesync.domain.model.Telemetry
import com.example.hykesync.wear.domain.communication.WearableCommunicator
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class WearableCommunicatorImpl(context: Context) : WearableCommunicator {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        private const val PHONE_CAPABILITY = "phone_app"
        private const val TELEMETRY_PATH = "/telemetry_batch"
    }

    override suspend fun sendTelemetryBatch(telemetryList: List<Telemetry>): Boolean {
        return try {
            val request = PutDataMapRequest.create(TELEMETRY_PATH).apply {
                val dataMapList = ArrayList<DataMap>()
                telemetryList.forEach { telemetry ->
                    val dm = DataMap().apply {
                        putLong("sessionId", telemetry.sessionId)
                        putLong("timestamp", telemetry.timestamp)
                        putFloat("heartRate", telemetry.heartRate)
                        putFloat("altitude", telemetry.altitude)
                        putFloat("pressure", telemetry.pressure)
                        putFloat("azimuth", telemetry.azimuth)
                        putFloat("pitch", telemetry.pitch)
                        putFloat("roll", telemetry.roll)
                        putString("connectionStatus", telemetry.connectionStatus)
                    }
                    dataMapList.add(dm)
                }
                dataMap.putDataMapArrayList("batch", dataMapList)
                setUrgent()
            }
            dataClient.putDataItem(request.asPutDataRequest()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendSessionCommand(path: String, sessionId: Long): Boolean {
        return try {
            val capabilityNodes = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes

            val nodes = if (capabilityNodes.isNotEmpty()) capabilityNodes else nodeClient.connectedNodes.await().toSet()

            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, sessionId.toString().toByteArray()).await()
            }
            nodes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
