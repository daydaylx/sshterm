package com.dlx.sshterm.data.local.db.mapper

import com.dlx.sshterm.data.local.db.entity.KnownHostEntity
import com.dlx.sshterm.domain.model.KnownHostEntry

/**
 * Mapper between KnownHostEntity and KnownHostEntry domain model.
 */
object KnownHostEntityMapper {

    fun toDomain(entity: KnownHostEntity): KnownHostEntry = KnownHostEntry(
        host = entity.host,
        algorithm = entity.algorithm,
        fingerprint = entity.fingerprint,
        trustDate = entity.trustDate
    )

    fun toEntity(domain: KnownHostEntry): KnownHostEntity = KnownHostEntity(
        host = domain.host,
        algorithm = domain.algorithm,
        fingerprint = domain.fingerprint,
        trustDate = domain.trustDate
    )

    fun toDomainList(entities: List<KnownHostEntity>): List<KnownHostEntry> =
        entities.map { toDomain(it) }

    fun toEntityList(domains: List<KnownHostEntry>): List<KnownHostEntity> =
        domains.map { toEntity(it) }
}
