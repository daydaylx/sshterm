package com.example.privatessh.data.repository

import com.example.privatessh.data.local.db.dao.KnownHostDao
import com.example.privatessh.data.local.db.mapper.KnownHostEntityMapper
import com.example.privatessh.domain.model.KnownHostEntry
import com.example.privatessh.domain.repository.KnownHostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for known SSH host keys.
 */
class KnownHostRepositoryImpl @Inject constructor(
    private val knownHostDao: KnownHostDao
) : KnownHostRepository {

    override fun observeKnownHosts(): Flow<List<KnownHostEntry>> =
        knownHostDao.observeAll().map { KnownHostEntityMapper.toDomainList(it) }

    override suspend fun getKnownHost(host: String): KnownHostEntry? =
        knownHostDao.getByHost(host)?.let { KnownHostEntityMapper.toDomain(it) }

    override suspend fun addKnownHost(entry: KnownHostEntry) {
        knownHostDao.insert(KnownHostEntityMapper.toEntity(entry))
    }

    override suspend fun isHostTrusted(host: String, fingerprint: String): Boolean =
        knownHostDao.isTrusted(host, fingerprint)

    override suspend fun removeKnownHost(host: String) {
        knownHostDao.deleteByHost(host)
    }

    override suspend fun clearAll() {
        knownHostDao.deleteAll()
    }
}
