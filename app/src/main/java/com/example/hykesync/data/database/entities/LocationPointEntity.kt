package com.example.hykesync.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.hykesync.domain.model.LocationPoint

@Entity(
    tableName = "location_points",
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
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
)

fun LocationPointEntity.toDomain() = LocationPoint(
    id = id,
    sessionId = sessionId,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude
)

fun LocationPoint.toEntity() = LocationPointEntity(
    id = id,
    sessionId = sessionId,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    isSynced = false
)
