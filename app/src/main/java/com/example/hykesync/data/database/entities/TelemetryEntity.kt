package com.example.hykesync.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.hykesync.domain.model.Telemetry

@Entity(
    tableName = "telemetry",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val heartRate: Float,
    val altitude: Float,
    val pressure: Float,
    val azimuth: Float,
    val pitch: Float,
    val roll: Float,
    val connectionStatus: String,
    val isSynced: Boolean = false
)

fun TelemetryEntity.toDomain() = Telemetry(
    id = id,
    sessionId = sessionId,
    timestamp = timestamp,
    heartRate = heartRate,
    altitude = altitude,
    pressure = pressure,
    azimuth = azimuth,
    pitch = pitch,
    roll = roll,
    connectionStatus = connectionStatus,
)

fun Telemetry.toEntity() = TelemetryEntity(
    id = id,
    sessionId = sessionId,
    timestamp = timestamp,
    heartRate = heartRate,
    altitude = altitude,
    pressure = pressure,
    azimuth = azimuth,
    pitch = pitch,
    roll = roll,
    connectionStatus = connectionStatus,
    isSynced = false
)
