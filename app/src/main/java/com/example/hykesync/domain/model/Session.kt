package com.example.hykesync.domain.model

/**
 * Representa una sesión de senderismo.
 *
 * @property id Identificador único de la sesión.
 * @property startTime Timestamp de inicio de la sesión (ms).
 * @property endTime Timestamp de fin de la sesión (ms), puede ser nulo si está activa.
 * @property status Estado de la sesión (ej. ACTIVE, COMPLETED, PAUSED).
 */
data class Session(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val status: SessionStatus
)

enum class SessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}
