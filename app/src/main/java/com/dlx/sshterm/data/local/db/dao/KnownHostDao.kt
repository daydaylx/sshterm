package com.dlx.sshterm.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dlx.sshterm.data.local.db.entity.KnownHostEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for known SSH host keys.
 */
@Dao
interface KnownHostDao {
    /**
     * Observes all known hosts.
     */
    @Query("SELECT * FROM known_hosts ORDER BY trustDate DESC")
    fun observeAll(): Flow<List<KnownHostEntity>>

    /**
     * Gets a known host entry by hostname (with port).
     */
    @Query("SELECT * FROM known_hosts WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): KnownHostEntity?

    /**
     * Checks if a host with the given fingerprint exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM known_hosts WHERE host = :host AND fingerprint = :fingerprint LIMIT 1)")
    suspend fun isTrusted(host: String, fingerprint: String): Boolean

    /**
     * Inserts or replaces a known host entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: KnownHostEntity)

    /**
     * Deletes a known host entry.
     */
    @Query("DELETE FROM known_hosts WHERE host = :host")
    suspend fun deleteByHost(host: String)

    /**
     * Deletes all known hosts.
     */
    @Query("DELETE FROM known_hosts")
    suspend fun deleteAll()
}
