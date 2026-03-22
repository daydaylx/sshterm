package com.example.privatessh.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.privatessh.data.local.db.entity.HostEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for SSH host profiles.
 */
@Dao
interface HostDao {
    /**
     * Observes all hosts ordered by creation date.
     */
    @Query("SELECT * FROM hosts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HostEntity>>

    /**
     * Gets a host by ID.
     */
    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getById(id: String): HostEntity?

    /**
     * Gets a host by hostname and port.
     */
    @Query("SELECT * FROM hosts WHERE host = :host AND port = :port LIMIT 1")
    suspend fun getByAddress(host: String, port: Int): HostEntity?

    /**
     * Gets hosts marked for connect on launch.
     */
    @Query("SELECT * FROM hosts WHERE connectOnLaunch = 1 ORDER BY createdAt DESC")
    suspend fun getConnectOnLaunch(): List<HostEntity>

    /**
     * Inserts or replaces a host.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(host: HostEntity)

    /**
     * Deletes a host.
     */
    @Delete
    suspend fun delete(host: HostEntity)

    /**
     * Deletes a host by ID.
     */
    @Query("DELETE FROM hosts WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Updates the last connected time for a host.
     */
    @Query("UPDATE hosts SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: Long)

    /**
     * Deletes all hosts.
     */
    @Query("DELETE FROM hosts")
    suspend fun deleteAll()
}
