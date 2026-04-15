package com.dlx.sshterm.data.repository

import com.dlx.sshterm.data.local.db.dao.HostDao
import com.dlx.sshterm.data.local.db.mapper.HostEntityMapper
import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.repository.HostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for SSH host profiles.
 */
class HostRepositoryImpl @Inject constructor(
    private val hostDao: HostDao
) : HostRepository {

    override fun observeHosts(): Flow<List<HostProfile>> =
        hostDao.observeAll().map { HostEntityMapper.toDomainList(it) }

    override suspend fun getHost(id: String): HostProfile? =
        hostDao.getById(id)?.let { HostEntityMapper.toDomain(it) }

    override suspend fun save(host: HostProfile) {
        hostDao.insert(HostEntityMapper.toEntity(host))
    }

    override suspend fun delete(id: String) {
        hostDao.deleteById(id)
    }

    override suspend fun getHostByAddress(host: String, port: Int): HostProfile? =
        hostDao.getByAddress(host, port)?.let { HostEntityMapper.toDomain(it) }

    /**
     * Updates the last connected time for a host.
     */
    suspend fun updateLastConnected(id: String, timestamp: Long = System.currentTimeMillis()) {
        hostDao.updateLastConnected(id, timestamp)
    }
}
