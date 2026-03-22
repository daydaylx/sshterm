package com.example.privatessh.ssh.auth

import com.example.privatessh.domain.repository.SecureKeyRepository
import com.example.privatessh.ssh.SshSessionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Private key authentication backed by encrypted local key storage.
 */
@Singleton
class PrivateKeyAuthStrategy @Inject constructor(
    private val secureKeyRepository: SecureKeyRepository
) : AuthStrategy {

    override suspend fun authenticate(
        client: net.schmizz.sshj.SSHClient,
        config: SshSessionConfig
    ): net.schmizz.sshj.SSHClient? = withContext(Dispatchers.IO) {
        try {
            val keyAlias = aliasForHost(config.hostProfile.id)
            val privateKeyPem = secureKeyRepository.getKey(keyAlias) ?: return@withContext null
            val keyProvider = client.loadKeys(privateKeyPem, null, null)
            client.authPublickey(config.getUsername(), keyProvider)
            client
        } catch (_: Exception) {
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
