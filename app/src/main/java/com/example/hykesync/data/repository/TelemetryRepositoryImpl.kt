package com.example.hykesync.data.repository

import com.example.hykesync.data.database.dao.TelemetryDao
import com.example.hykesync.data.database.entities.toDomain
import com.example.hykesync.data.database.entities.toEntity
import com.example.hykesync.domain.model.Telemetry
import com.example.hykesync.domain.repository.TelemetryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TelemetryRepositoryImpl(private val telemetryDao: TelemetryDao) : TelemetryRepository {
    override suspend fun saveTelemetry(telemetry: Telemetry) {
        telemetryDao.insertTelemetry(telemetry.toEntity())
    }

    override suspend fun saveTelemetryBatch(telemetryList: List<Telemetry>) {
        telemetryList.forEach { telemetry ->
            telemetryDao.insertTelemetry(telemetry.toEntity())
        }
    }

    override fun getTelemetryForSession(sessionId: Long): Flow<List<Telemetry>> {
        return telemetryDao.getTelemetryForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
