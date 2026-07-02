package com.example.hykesync.wear.data.communication

import android.content.Context
import com.example.hykesync.wear.domain.communication.NodeDiscovery
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class NodeDiscoveryImpl(context: Context) : NodeDiscovery {
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        private const val PHONE_CAPABILITY = "phone_app"
    }

    override suspend fun isPhoneAvailable(): Boolean {
        return try {
            val capabilityNodes = capabilityClient
                .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
            if (capabilityNodes.isNotEmpty()) {
                true
            } else {
                nodeClient.connectedNodes.await().isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }
}
