package com.dlx.sshterm.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.NetworkTargetType

/**
 * Room entity for SSH host profiles.
 */
@Entity(
    tableName = "hosts",
    indices = [
        Index(value = ["host", "port"], name = "idx_host_port"),
        Index(value = ["createdAt"], name = "idx_created_at")
    ]
)
data class HostEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val user: String,
    val authType: AuthType,
    val targetType: NetworkTargetType,
    val createdAt: Long,
    val lastConnectedAt: Long?,
    val connectOnLaunch: Boolean
)
