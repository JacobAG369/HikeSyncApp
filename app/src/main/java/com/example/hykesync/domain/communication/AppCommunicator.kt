package com.example.hykesync.domain.communication

/**
 * Interfaz para enviar comandos desde el teléfono al reloj.
 */
interface AppCommunicator {
    /**
     * Envía un comando para iniciar o detener la sesión en el reloj.
     */
    suspend fun sendSessionCommand(path: String, sessionId: Long): Boolean

    suspend fun getReachableWearNodeName(): String?
}
