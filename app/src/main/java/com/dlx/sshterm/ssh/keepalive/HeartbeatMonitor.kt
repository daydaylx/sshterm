package com.dlx.sshterm.ssh.keepalive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors SSH session health via heartbeat checks.
 */
@Singleton
class HeartbeatMonitor @Inject constructor(

) {

    private val _isAlive = MutableStateFlow(true)
    val isAlive: StateFlow<Boolean> = _isAlive.asStateFlow()

    private var monitorScope: CoroutineScope? = null
    private var monitorJob: Job? = null

    private var lastHeartbeatTime = 0L
    private val heartbeatTimeoutMs = 60_000L // 60 seconds

    /**
     * Starts monitoring heartbeat.
     */
    fun startMonitoring(scope: CoroutineScope, intervalMs: Long = 30_000L) {
        monitorScope = scope
        monitorJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                checkHeartbeat()
            }
        }
    }

    /**
     * Stops monitoring heartbeat.
     */
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorScope = null
        monitorJob = null
    }

    /**
     * Records a heartbeat signal.
     * Should be called when data is received from the SSH session.
     */
    fun recordHeartbeat() {
        lastHeartbeatTime = System.currentTimeMillis()
        _isAlive.value = true
    }

    /**
     * Checks if heartbeat timeout has occurred.
     */
    private fun checkHeartbeat() {
        val timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime
        if (timeSinceLastHeartbeat > heartbeatTimeoutMs) {
            _isAlive.value = false
        }
    }

    /**
     * Resets the heartbeat state.
     */
    fun reset() {
        lastHeartbeatTime = System.currentTimeMillis()
        _isAlive.value = true
    }
}
