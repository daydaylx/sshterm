package com.example.privatessh.ssh.keepalive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
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
    private var onKeepAliveFailure: ((Throwable) -> Unit)? = null
    private var failureReported: Boolean = false

    private var currentClient: SSHClient? = null
    private var intervalMs: Long = 30_000L // Default 30 seconds

    /** Exposed so callers can react when the heartbeat times out. */
    val isAlive: StateFlow<Boolean> get() = heartbeatMonitor.isAlive

    /**
     * Starts sending keep-alive packets.
     * [onConnectionLost] is invoked once when the heartbeat monitor detects a stale connection.
     */
    fun startKeepAlive(
        scope: CoroutineScope,
        client: SSHClient,
        intervalMs: Long = 30_000L,
        onConnectionLost: () -> Unit = {},
        onKeepAliveFailure: (Throwable) -> Unit = {}
    ) {
        this.currentClient = client
        this.intervalMs = intervalMs
        this.onKeepAliveFailure = onKeepAliveFailure
        failureReported = false

        keepAliveScope = scope
        keepAliveJob = scope.launch {
            while (true) {
                delay(intervalMs)
                sendKeepAlive()
            }
        }

        // Start heartbeat monitoring
        heartbeatMonitor.startMonitoring(scope, intervalMs * 2)

        // Trigger disconnect callback when heartbeat is lost (skip the initial true emission)
        scope.launch {
            isAlive.drop(1).collect { alive ->
                if (!alive) onConnectionLost()
            }
        }
    }

    /**
     * Stops sending keep-alive packets.
     */
    fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveScope = null
        keepAliveJob = null
        currentClient = null
        onKeepAliveFailure = null
        failureReported = false

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
                reportFailure(IllegalStateException("SSH keep-alive detected a disconnected client."))
                return
            }

            // Record heartbeat
            heartbeatMonitor.recordHeartbeat()
            failureReported = false
        } catch (e: ConnectionException) {
            reportFailure(e)
        } catch (e: Exception) {
            reportFailure(e)
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

    private fun reportFailure(throwable: Throwable) {
        if (failureReported) {
            return
        }

        failureReported = true
        onKeepAliveFailure?.invoke(throwable)
    }
}
