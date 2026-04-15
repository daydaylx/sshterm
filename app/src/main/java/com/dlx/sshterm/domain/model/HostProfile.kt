package com.dlx.sshterm.domain.model

import java.util.UUID

/**
 * SSH host connection profile.
 */
data class HostProfile(
    val id: String = generateId(),
    val name: String,
    val host: String,
    val port: Int = DEFAULT_PORT,
    val user: String,
    val authType: AuthType,
    val targetType: NetworkTargetType = NetworkTargetType.DIRECT,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null,
    val connectOnLaunch: Boolean = false
) {
    companion object {
        const val DEFAULT_PORT = 22
        const val MIN_PORT = 1
        const val MAX_PORT = 65535

        fun generateId(): String = UUID.randomUUID().toString()
    }

    /**
     * Returns the host:port string for SSH connection.
     */
    fun getHostWithPort(): String = "$host:$port"

    /**
     * Validates the profile has all required fields.
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
            host.isNotBlank() &&
            user.isNotBlank() &&
            port in MIN_PORT..MAX_PORT
    }

    /**
     * Checks if this is a Tailscale host.
     */
    fun isTailscale(): Boolean = targetType == NetworkTargetType.TAILSCALE

    /**
     * Returns display name with user@host format.
     */
    fun getDisplayName(): String = "$user@$host"

}
