package com.example.privatessh.ssh.auth

import com.example.privatessh.ssh.SshSessionConfig
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Password-based authentication with in-memory session caching only.
 */
@Singleton
class PasswordAuthStrategy @Inject constructor() : AuthStrategy {

    private val passwordCache = ConcurrentHashMap<String, String>()

    override suspend fun authenticate(
        client: net.schmizz.sshj.SSHClient,
        config: SshSessionConfig
    ): net.schmizz.sshj.SSHClient? {
        val password = passwordCache[config.hostProfile.id]?.takeIf { it.isNotBlank() } ?: return null

        return try {
            client.authPassword(config.getUsername(), password)
            client
        } catch (_: Exception) {
            null
        }
    }

    fun setPassword(hostId: String, password: String) {
        if (password.isBlank()) {
            passwordCache.remove(hostId)
        } else {
            passwordCache[hostId] = password
        }
    }

    fun hasPassword(hostId: String): Boolean = !passwordCache[hostId].isNullOrBlank()

    fun clearPassword(hostId: String) {
        passwordCache.remove(hostId)
    }
}
