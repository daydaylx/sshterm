package com.example.privatessh.ssh.io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens and manages the interactive shell channel for a session.
 */
@Singleton
class ShellChannelAdapter @Inject constructor() {

    data class ShellHandle(
        val session: Session,
        val shell: Session.Shell
    )

    suspend fun openShellChannel(
        client: SSHClient,
        terminalType: String = "xterm-256color",
        columns: Int = 80,
        rows: Int = 24
    ): ShellHandle? = withContext(Dispatchers.IO) {
        try {
            val session = client.startSession()
            session.allocatePTY(terminalType, columns, rows, 0, 0, emptyMap())
            val shell = session.startShell()
            ShellHandle(session = session, shell = shell)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun closeChannel(handle: ShellHandle) = withContext(Dispatchers.IO) {
        try {
            handle.shell.close()
        } catch (_: Exception) {
        }

        try {
            handle.session.close()
        } catch (_: Exception) {
        }
    }

    suspend fun resizePty(handle: ShellHandle, columns: Int, rows: Int) = withContext(Dispatchers.IO) {
        try {
            handle.shell.changeWindowDimensions(columns, rows, 0, 0)
        } catch (_: Exception) {
        }
    }
}
