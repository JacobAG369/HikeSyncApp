package com.example.hykesync.wear.domain.communication

/**
 * Interfaz para el descubrimiento de nodos (dispositivos) en la red Wearable.
 */
interface NodeDiscovery {
    /**
     * Comprueba si hay un teléfono conectado con la aplicación instalada.
     */
    suspend fun isPhoneAvailable(): Boolean
}
