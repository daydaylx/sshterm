package com.dlx.sshterm.ssh.auth

import com.dlx.sshterm.ssh.SshSessionConfig

/**
 * Authentication strategy interface.
 */
interface AuthStrategy {

    /**
     * Authenticates the SSH session using the configured method.
     * @return The authenticated SSH client, or null if authentication failed.
     */
    suspend fun authenticate(
        client: net.schmizz.sshj.SSHClient,
        config: SshSessionConfig
    ): net.schmizz.sshj.SSHClient?
}
