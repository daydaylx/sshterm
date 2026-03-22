package com.example.privatessh.data.local.db.mapper

import com.example.privatessh.data.local.db.entity.SessionSnapshotEntity
import com.example.privatessh.domain.model.SessionSnapshot

/**
 * Mapper between SessionSnapshotEntity and SessionSnapshot domain model.
 */
object SessionSnapshotMapper {

    fun toDomain(entity: SessionSnapshotEntity): SessionSnapshot = SessionSnapshot(
        sessionId = entity.sessionId,
        hostId = entity.hostId,
        status = entity.status,
        startTime = entity.startTime,
        lastActivityTime = entity.lastActivityTime,
        bytesReceived = entity.bytesReceived,
        bytesSent = entity.bytesSent,
        errorMessage = entity.errorMessage
    )

    fun toEntity(domain: SessionSnapshot): SessionSnapshotEntity = SessionSnapshotEntity(
        sessionId = domain.sessionId,
        hostId = domain.hostId,
        status = domain.status,
        startTime = domain.startTime,
        lastActivityTime = domain.lastActivityTime,
        bytesReceived = domain.bytesReceived,
        bytesSent = domain.bytesSent,
        errorMessage = domain.errorMessage
    )
}
