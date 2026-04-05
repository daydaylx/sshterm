package com.example.privatessh.ssh.auth

import com.example.privatessh.diagnostics.DiagnosticCategory
import com.example.privatessh.diagnostics.SessionDiagnosticsStore
import com.example.privatessh.ssh.SshSessionConfig
import net.schmizz.sshj.userauth.UserAuthException
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Password-based authentication with in-memory session caching only.
 * Passwords are stored as CharArray for best-effort zeroing on clear.
 */
@Singleton
class PasswordAuthStrategy @Inject constructor(
    private val diagnosticsStore: SessionDiagnosticsStore
) : AuthStrategy {

    private val passwordCache = ConcurrentHashMap<String, CharArray>()

    override suspend fun authenticate(
        client: net.schmizz.sshj.SSHClient,
        config: SshSessionConfig
    ): net.schmizz.sshj.SSHClient? {
        val passwordChars = passwordCache[config.hostProfile.id]
            ?.takeIf { arr -> arr.any { it != '\u0000' } }
            ?: return null

        return try {
            client.authPassword(config.getUsername(), String(passwordChars))
            client
        } catch (e: UserAuthException) {
            Timber.w("Password auth rejected for host %s", config.hostProfile.id)
            diagnosticsStore.warn(
                category = DiagnosticCategory.AUTH,
                title = "Passwortauthentifizierung abgelehnt",
                detail = "Benutzer: ${config.getUsername()}",
                throwable = e,
                sessionId = config.hostProfile.id,
                hostId = config.hostProfile.id,
                hostName = config.hostProfile.getDisplayName()
            )
            null
        } catch (e: Exception) {
            Timber.w(e, "Password auth I/O error for host %s", config.hostProfile.id)
            diagnosticsStore.error(
                category = DiagnosticCategory.AUTH,
                title = "Passwortauthentifizierung fehlgeschlagen",
                detail = "Benutzer: ${config.getUsername()}",
                throwable = e,
                sessionId = config.hostProfile.id,
                hostId = config.hostProfile.id,
                hostName = config.hostProfile.getDisplayName()
            )
            null
        }
    }

    fun setPassword(hostId: String, password: String) {
        if (password.isBlank()) {
            clearPassword(hostId)
        } else {
            passwordCache[hostId] = password.toCharArray()
        }
    }

    fun hasPassword(hostId: String): Boolean {
        val arr = passwordCache[hostId] ?: return false
        return arr.any { it != '\u0000' }
    }

    fun clearPassword(hostId: String) {
        passwordCache.remove(hostId)?.fill('\u0000')
    }
}
