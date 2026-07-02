package com.example.hykesync.data.communication

import android.content.Context
import android.util.Log
import com.example.hykesync.data.repository.SessionRepositoryImpl
import com.example.hykesync.domain.communication.AppCommunicator
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

class AppCommunicatorImpl(context: Context) : AppCommunicator {
    private val sessionRepository = SessionRepositoryImpl(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    override suspend fun sendSessionCommand(path: String, sessionId: Long): Boolean {
        return try {
            val nodes = getReachableNodes()
            if (nodes.isEmpty()) {
                return false
            }

            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, sessionId.toString().toByteArray()).await()
            }

            when (path) {
                "/start_session" -> sessionRepository.startNewSession(sessionId)
                "/stop_session" -> sessionRepository.stopActiveSession()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getReachableWearNodeName(): String? {
        return try {
            getReachableNodes().firstOrNull()?.displayName
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getReachableNodes(): Collection<Node> {
        val capabilityNodes = capabilityClient
            .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes

        if (capabilityNodes.isNotEmpty()) {
            return capabilityNodes
        }

        val connectedNodes = nodeClient.connectedNodes.await()
        if (connectedNodes.isNotEmpty()) {
            Log.w(TAG, "Capability '$WEAR_CAPABILITY' sin nodos; usando NodeClient fallback")
        }
        return connectedNodes
    }

    companion object {
        private const val WEAR_CAPABILITY = "wear_app"
        private const val TAG = "AppCommunicator"
    }
}
