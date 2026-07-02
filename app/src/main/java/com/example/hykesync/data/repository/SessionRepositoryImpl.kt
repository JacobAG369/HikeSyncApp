package com.example.hykesync.data.repository

import android.content.Context
import com.example.hykesync.data.database.HikeSyncDatabase
import com.example.hykesync.data.database.entities.SessionEntity
import com.example.hykesync.domain.model.SessionStatus

class SessionRepositoryImpl(context: Context) {
    private val appContext = context.applicationContext
    private val sessionDao = HikeSyncDatabase.getDatabase(appContext).sessionDao()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun startNewSession(sessionId: Long = System.currentTimeMillis()): Long {
        val session = SessionEntity(
            id = sessionId,
            startTime = System.currentTimeMillis(),
            endTime = null,
            status = SessionStatus.ACTIVE.name
        )
        sessionDao.insertSession(session)
        setActiveSessionId(sessionId)
        return sessionId
    }

    suspend fun stopActiveSession(endTime: Long = System.currentTimeMillis()) {
        val activeSessionId = getActiveSessionId() ?: return
        val session = sessionDao.getSessionById(activeSessionId) ?: return
        sessionDao.updateSession(
            session.copy(
                endTime = endTime,
                status = SessionStatus.COMPLETED.name
            )
        )
        clearActiveSessionId()
    }

    fun getActiveSessionId(): Long? {
        val sessionId = prefs.getLong(KEY_ACTIVE_SESSION_ID, NO_SESSION_ID)
        return sessionId.takeIf { it != NO_SESSION_ID }
    }

    fun clearActiveSessionId() {
        prefs.edit().remove(KEY_ACTIVE_SESSION_ID).apply()
    }

    private fun setActiveSessionId(sessionId: Long) {
        prefs.edit().putLong(KEY_ACTIVE_SESSION_ID, sessionId).apply()
    }

    companion object {
        private const val PREFS_NAME = "hikesync_session_prefs"
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val NO_SESSION_ID = -1L
    }
}
