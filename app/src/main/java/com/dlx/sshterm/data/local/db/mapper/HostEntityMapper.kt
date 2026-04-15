package com.dlx.sshterm.data.local.db.mapper

import com.dlx.sshterm.data.local.db.entity.HostEntity
import com.dlx.sshterm.domain.model.HostProfile

/**
 * Mapper between HostEntity and HostProfile domain model.
 */
object HostEntityMapper {

    fun toDomain(entity: HostEntity): HostProfile = HostProfile(
        id = entity.id,
        name = entity.name,
        host = entity.host,
        port = entity.port,
        user = entity.user,
        authType = entity.authType,
        targetType = entity.targetType,
        createdAt = entity.createdAt,
        lastConnectedAt = entity.lastConnectedAt,
        connectOnLaunch = entity.connectOnLaunch
    )

    fun toEntity(domain: HostProfile): HostEntity = HostEntity(
        id = domain.id,
        name = domain.name,
        host = domain.host,
        port = domain.port,
        user = domain.user,
        authType = domain.authType,
        targetType = domain.targetType,
        createdAt = domain.createdAt,
        lastConnectedAt = domain.lastConnectedAt,
        connectOnLaunch = domain.connectOnLaunch
    )

    fun toDomainList(entities: List<HostEntity>): List<HostProfile> =
        entities.map { toDomain(it) }

    fun toEntityList(domains: List<HostProfile>): List<HostEntity> =
        domains.map { toEntity(it) }
}
