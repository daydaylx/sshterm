package com.dlx.sshterm.domain.repository

import com.dlx.sshterm.domain.model.KnownHostEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository for known SSH host keys.
 */
interface KnownHostRepository {
    /**
     * Observes all known hosts.
     */
    fun observeKnownHosts(): Flow<List<KnownHostEntry>>

    /**
     * Gets a known host entry by hostname (with port).
     */
    suspend fun getKnownHost(host: String): KnownHostEntry?

    /**
     * Adds a new known host entry.
     */
    suspend fun addKnownHost(entry: KnownHostEntry)

    /**
     * Checks if a host is trusted with the given fingerprint.
     */
    suspend fun isHostTrusted(host: String, fingerprint: String): Boolean

    /**
     * Removes a known host entry.
     */
    suspend fun removeKnownHost(host: String)

    /**
     * Clears all known hosts.
     */
    suspend fun clearAll()
}
