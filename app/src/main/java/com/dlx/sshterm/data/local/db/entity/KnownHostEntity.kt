package com.dlx.sshterm.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for known SSH host keys.
 */
@Entity(
    tableName = "known_hosts",
    indices = [
        Index(value = ["host"], name = "idx_host", unique = true)
    ]
)
data class KnownHostEntity(
    @PrimaryKey
    val host: String,
    val algorithm: String,
    val fingerprint: String,
    val trustDate: Long
)
