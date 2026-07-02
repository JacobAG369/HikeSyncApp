package com.example.hykesync.data.database.dao

import androidx.room.*
import com.example.hykesync.data.database.entities.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(locationPoint: LocationPointEntity)

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLocationPointsForSession(sessionId: Long): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocationPointForSession(sessionId: Long): Flow<LocationPointEntity?>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocationPointSnapshotForSession(sessionId: Long): LocationPointEntity?

    @Query("SELECT COUNT(*) FROM location_points WHERE sessionId = :sessionId")
    suspend fun getLocationCountForSession(sessionId: Long): Int

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLocationPointListForSession(sessionId: Long): List<LocationPointEntity>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId AND isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocationPointsForSession(sessionId: Long): List<LocationPointEntity>

    @Query("UPDATE location_points SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionLocationPointsAsSynced(sessionId: Long)

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deleteLocationPointsForSession(sessionId: Long)
}
