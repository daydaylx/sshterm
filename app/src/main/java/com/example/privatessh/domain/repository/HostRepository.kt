package com.example.privatessh.domain.repository

import com.example.privatessh.domain.model.HostProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository for SSH host profiles.
 */
interface HostRepository {
    /**
     * Observes all hosts as a Flow.
     */
    fun observeHosts(): Flow<List<HostProfile>>

    /**
     * Gets a host by ID.
     */
    suspend fun getHost(id: String): HostProfile?

    /**
     * Saves a host (create or update).
     */
    suspend fun save(host: HostProfile)

    /**
     * Deletes a host by ID.
     */
    suspend fun delete(id: String)

    /**
     * Gets a host by hostname and port.
     */
    suspend fun getHostByAddress(host: String, port: Int): HostProfile?
}
