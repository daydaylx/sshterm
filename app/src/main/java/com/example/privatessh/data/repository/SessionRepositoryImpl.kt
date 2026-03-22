package com.example.privatessh.data.repository

import com.example.privatessh.data.local.db.dao.SessionSnapshotDao
import com.example.privatessh.data.local.db.mapper.SessionSnapshotMapper
import com.example.privatessh.domain.model.SessionSnapshot
import com.example.privatessh.domain.model.SessionStatus
import com.example.privatessh.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for session state.
 */
class SessionRepositoryImpl @Inject constructor(
    private val sessionSnapshotDao: SessionSnapshotDao
) : SessionRepository {

    override fun observeSession(hostId: String): Flow<SessionSnapshot?> =
        sessionSnapshotDao.observeByHostId(hostId).map { entity ->
            entity?.let { SessionSnapshotMapper.toDomain(it) }
        }

    override fun observeActiveSessions(): Flow<List<SessionSnapshot>> =
        sessionSnapshotDao.observeActiveSessions().map { entities ->
            entities.map { SessionSnapshotMapper.toDomain(it) }
        }

    override suspend fun saveSnapshot(snapshot: SessionSnapshot) {
        sessionSnapshotDao.insert(SessionSnapshotMapper.toEntity(snapshot))
    }

    override suspend fun updateStatus(sessionId: String, status: SessionStatus) {
        sessionSnapshotDao.updateStatus(sessionId, status)
    }

    override suspend fun updateActivity(sessionId: String) {
        sessionSnapshotDao.updateActivity(sessionId, System.currentTimeMillis())
    }

    override suspend fun deleteSnapshot(sessionId: String) {
        sessionSnapshotDao.deleteById(sessionId)
    }

    override suspend fun getSession(sessionId: String): SessionSnapshot? =
        sessionSnapshotDao.getById(sessionId)?.let { SessionSnapshotMapper.toDomain(it) }

    /**
     * Sets error for a session.
     */
    suspend fun setError(sessionId: String, error: String?) {
        sessionSnapshotDao.setError(sessionId, error)
    }

    /**
     * Deletes old sessions.
     */
    suspend fun deleteOlderThan(timestamp: Long) {
        sessionSnapshotDao.deleteOlderThan(timestamp)
    }
}
