package com.example.privatessh.ssh

import com.example.privatessh.domain.model.HostProfile
import net.schmizz.sshj.SSHClient
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
        val client = SSHClient()
        client.setConnectTimeout(15_000)
        client.setTimeout(30_000)
        return client
    }
}
