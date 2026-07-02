package com.example.hykesync.data.database.dao

import androidx.room.*
import com.example.hykesync.data.database.entities.TelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: TelemetryEntity)

    @Query("SELECT * FROM telemetry WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getTelemetryForSession(sessionId: Long): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getTelemetryListForSession(sessionId: Long): List<TelemetryEntity>

    @Query("SELECT * FROM telemetry WHERE sessionId = :sessionId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedTelemetryForSession(sessionId: Long): List<TelemetryEntity>

    @Query("SELECT * FROM telemetry WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestTelemetryForSession(sessionId: Long): Flow<TelemetryEntity?>

    @Query("SELECT * FROM telemetry WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTelemetrySnapshotForSession(sessionId: Long): TelemetryEntity?

    @Query("SELECT COUNT(*) FROM telemetry WHERE sessionId = :sessionId")
    suspend fun getTelemetryCountForSession(sessionId: Long): Int

    @Query("UPDATE telemetry SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionTelemetryAsSynced(sessionId: Long)

    @Query("DELETE FROM telemetry WHERE sessionId = :sessionId")
    suspend fun deleteTelemetryForSession(sessionId: Long)
}
