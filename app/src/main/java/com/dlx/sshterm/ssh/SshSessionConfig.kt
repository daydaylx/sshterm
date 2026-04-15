package com.dlx.sshterm.ssh

import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.HostProfile

/**
 * Configuration for an SSH session.
 */
data class SshSessionConfig(
    val hostProfile: HostProfile,
    val terminalColumns: Int = 80,
    val terminalRows: Int = 24,
    val terminalType: String = "xterm-256color",
    val keepAliveInterval: Int = 30, // seconds
    val connectionTimeout: Int = 15, // seconds
    val readTimeout: Int = 30 // seconds
) {
    /**
     * Returns the host:port for connection.
     */
    fun getHostWithPort(): String = "${hostProfile.host}:${hostProfile.port}"

    /**
     * Returns the username for authentication.
     */
    fun getUsername(): String = hostProfile.user

    /**
     * Returns the authentication type.
     */
    fun getAuthType(): AuthType = hostProfile.authType

    /**
     * Returns the hostname for display.
     */
    fun getDisplayName(): String = hostProfile.getDisplayName()
}
