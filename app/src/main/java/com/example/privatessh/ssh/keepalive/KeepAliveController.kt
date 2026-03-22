package com.example.privatessh.ssh.keepalive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.ConnectionException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for sending SSH keep-alive packets.
 */
@Singleton
class KeepAliveController @Inject constructor(
    private val heartbeatMonitor: HeartbeatMonitor
) {

    private var keepAliveScope: CoroutineScope? = null
    private var keepAliveJob: Job? = null

    private var currentClient: SSHClient? = null
    private var intervalMs: Long = 30_000L // Default 30 seconds

    /**
     * Starts sending keep-alive packets.
     */
    fun startKeepAlive(scope: CoroutineScope, client: SSHClient, intervalMs: Long = 30_000L) {
        this.currentClient = client
        this.intervalMs = intervalMs

        keepAliveScope = scope
        keepAliveJob = scope.launch {
            while (true) {
                delay(intervalMs)
                sendKeepAlive()
            }
        }

        // Start heartbeat monitoring
        heartbeatMonitor.startMonitoring(scope, intervalMs * 2)
    }

    /**
     * Stops sending keep-alive packets.
     */
    fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveScope = null
        keepAliveJob = null
        currentClient = null

        heartbeatMonitor.stopMonitoring()
    }

    /**
     * Sends a single keep-alive packet.
     */
    private fun sendKeepAlive() {
        val client = currentClient ?: return

        try {
            // SSHJ sends keep-alive packets automatically when keepAliveInterval is set
            // We just need to detect if the connection is still alive
            val isConnected = client.isConnected
            if (!isConnected) {
                // Connection lost
                return
            }

            // Record heartbeat
            heartbeatMonitor.recordHeartbeat()
        } catch (e: ConnectionException) {
            // Connection lost
        } catch (e: Exception) {
            // Ignore other errors
        }
    }

    /**
     * Updates the keep-alive interval.
     */
    fun updateInterval(intervalMs: Long) {
        this.intervalMs = intervalMs
    }

    /**
     * Resets the keep-alive state.
     */
    fun reset() {
        heartbeatMonitor.reset()
    }
}
