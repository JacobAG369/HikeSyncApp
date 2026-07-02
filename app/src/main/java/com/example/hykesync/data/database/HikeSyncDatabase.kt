package com.example.hykesync.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hykesync.data.database.dao.LocationPointDao
import com.example.hykesync.data.database.dao.SessionDao
import com.example.hykesync.data.database.dao.TelemetryDao
import com.example.hykesync.data.database.entities.LocationPointEntity
import com.example.hykesync.data.database.entities.SessionEntity
import com.example.hykesync.data.database.entities.TelemetryEntity

@Database(
    entities = [
        SessionEntity::class,
        TelemetryEntity::class,
        LocationPointEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HikeSyncDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun locationPointDao(): LocationPointDao

    companion object {
        @Volatile
        private var INSTANCE: HikeSyncDatabase? = null

        fun getDatabase(context: android.content.Context): HikeSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    HikeSyncDatabase::class.java,
                    "hikesync_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
