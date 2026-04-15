package com.dlx.sshterm.ssh.auth

import com.dlx.sshterm.diagnostics.DiagnosticCategory
import com.dlx.sshterm.diagnostics.SessionDiagnosticsStore
import com.dlx.sshterm.domain.repository.SecureKeyRepository
import com.dlx.sshterm.ssh.SshSessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Private key authentication backed by encrypted local key storage.
 */
@Singleton
class PrivateKeyAuthStrategy @Inject constructor(
    private val secureKeyRepository: SecureKeyRepository,
    private val diagnosticsStore: SessionDiagnosticsStore
) : AuthStrategy {

    override suspend fun authenticate(
        client: net.schmizz.sshj.SSHClient,
        config: SshSessionConfig
    ): net.schmizz.sshj.SSHClient? = withContext(Dispatchers.IO) {
        try {
            val keyAlias = aliasForHost(config.hostProfile.id)
            val privateKeyPem = secureKeyRepository.getKey(keyAlias) ?: run {
                diagnosticsStore.warn(
                    category = DiagnosticCategory.AUTH,
                    title = "Privater Schlüssel nicht gefunden",
                    detail = "Kein Schlüssel für diesen Host im sicheren Speicher vorhanden.",
                    sessionId = config.hostProfile.id,
                    hostId = config.hostProfile.id,
                    hostName = config.hostProfile.getDisplayName()
                )
                return@withContext null
            }
            val keyProvider = client.loadKeys(privateKeyPem, null, null)
            client.authPublickey(config.getUsername(), keyProvider)
            client
        } catch (e: Exception) {
            diagnosticsStore.error(
                category = DiagnosticCategory.AUTH,
                title = "Private-Key-Authentifizierung fehlgeschlagen",
                detail = "Benutzer: ${config.getUsername()}",
                throwable = e,
                sessionId = config.hostProfile.id,
                hostId = config.hostProfile.id,
                hostName = config.hostProfile.getDisplayName()
            )
            null
        }
    }

    suspend fun storePrivateKey(hostId: String, privateKeyPem: String): Boolean =
        try {
            secureKeyRepository.storeKey(aliasForHost(hostId), privateKeyPem)
            true
        } catch (_: Exception) {
            false
        }

    suspend fun clearPrivateKey(hostId: String) {
        try {
            secureKeyRepository.deleteKey(aliasForHost(hostId))
        } catch (_: Exception) { }
    }

    suspend fun hasPrivateKey(hostId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            secureKeyRepository.hasKey(aliasForHost(hostId))
        } catch (_: Exception) {
            false
        }
    }

    private fun aliasForHost(hostId: String): String = "host_$hostId"
}
