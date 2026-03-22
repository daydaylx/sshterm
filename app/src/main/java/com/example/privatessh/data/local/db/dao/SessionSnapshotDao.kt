package com.example.privatessh.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.privatessh.data.local.db.entity.SessionSnapshotEntity
import com.example.privatessh.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for session snapshots.
 */
@Dao
interface SessionSnapshotDao {
    /**
     * Observes all sessions for a specific host.
     */
    @Query("SELECT * FROM sessions WHERE hostId = :hostId ORDER BY startTime DESC LIMIT 1")
    fun observeByHostId(hostId: String): Flow<SessionSnapshotEntity?>

    /**
     * Observes all active sessions.
     */
    @Query("SELECT * FROM sessions WHERE status IN ('CONNECTED', 'GRACE_PERIOD') ORDER BY startTime DESC")
    fun observeActiveSessions(): Flow<List<SessionSnapshotEntity>>

    /**
     * Gets a session by ID.
     */
    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): SessionSnapshotEntity?

    /**
     * Inserts or replaces a session snapshot.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: SessionSnapshotEntity)

    /**
     * Updates the status of a session.
     */
    @Query("UPDATE sessions SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateStatus(sessionId: String, status: SessionStatus)

    /**
     * Updates the last activity time of a session.
     */
    @Query("UPDATE sessions SET lastActivityTime = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateActivity(sessionId: String, timestamp: Long)

    /**
     * Updates the error message for a session.
     */
    @Query("UPDATE sessions SET errorMessage = :error, status = 'ERROR' WHERE sessionId = :sessionId")
    suspend fun setError(sessionId: String, error: String?)

    /**
     * Deletes a session snapshot.
     */
    @Delete
    suspend fun delete(snapshot: SessionSnapshotEntity)

    /**
     * Deletes a session by ID.
     */
    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun deleteById(sessionId: String)

    /**
     * Deletes all sessions for a host.
     */
    @Query("DELETE FROM sessions WHERE hostId = :hostId")
    suspend fun deleteByHostId(hostId: String)

    /**
     * Deletes old sessions (older than specified timestamp).
     */
    @Query("DELETE FROM sessions WHERE startTime < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)
}
