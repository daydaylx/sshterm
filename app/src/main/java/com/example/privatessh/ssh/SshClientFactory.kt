package com.example.privatessh.ssh

import com.example.privatessh.domain.model.HostProfile
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating configured SSHJ clients.
 */
@Singleton
class SshClientFactory @Inject constructor(

) {

    /**
     * Creates a new SSH client configured for the given host profile.
     */
    fun createClient(hostProfile: HostProfile): SSHClient {
        return try {
            SecurityUtils.setRegisterBouncyCastle(false)
            val client = SSHClient()
            client.setConnectTimeout(15_000)
            client.setTimeout(30_000)
            client
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create SSH client: ${e.message}", e)
        }
    }
}
