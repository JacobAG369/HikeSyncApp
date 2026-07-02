package com.example.hykesync.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hykesync.domain.model.Session
import com.example.hykesync.domain.model.SessionStatus

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val status: String
)

fun SessionEntity.toDomain() = Session(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = SessionStatus.valueOf(status)
)

fun Session.toEntity() = SessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = status.name
)
