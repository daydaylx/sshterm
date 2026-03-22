package com.example.privatessh.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.privatessh.domain.model.SessionStatus

/**
 * Room entity for session snapshots.
 */
@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["hostId"], name = "idx_host_id"),
        Index(value = ["status"], name = "idx_status"),
        Index(value = ["startTime"], name = "idx_start_time")
    ]
)
data class SessionSnapshotEntity(
    @PrimaryKey
    val sessionId: String,
    val hostId: String,
    val status: SessionStatus,
    val startTime: Long,
    val lastActivityTime: Long,
    val bytesReceived: Long,
    val bytesSent: Long,
    val errorMessage: String?
)
