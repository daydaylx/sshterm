package com.example.privatessh.domain.repository

import com.example.privatessh.domain.model.SessionSnapshot
import com.example.privatessh.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for session state and snapshots.
 */
interface SessionRepository {
    /**
     * Observes the current session for a host.
     */
    fun observeSession(hostId: String): Flow<SessionSnapshot?>

    /**
     * Observes all active sessions.
     */
    fun observeActiveSessions(): Flow<List<SessionSnapshot>>

    /**
     * Saves or updates a session snapshot.
     */
    suspend fun saveSnapshot(snapshot: SessionSnapshot)

    /**
     * Updates the status of a session.
     */
    suspend fun updateStatus(sessionId: String, status: SessionStatus)

    /**
     * Updates the last activity time of a session.
     */
    suspend fun updateActivity(sessionId: String)

    /**
     * Deletes a session snapshot.
     */
    suspend fun deleteSnapshot(sessionId: String)

    /**
     * Gets a session by ID.
     */
    suspend fun getSession(sessionId: String): SessionSnapshot?
}
