package com.example.privatessh.ssh.reconnect

import com.example.privatessh.ssh.SshSessionEngine
import com.example.privatessh.ssh.SshSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing SSH session reconnection.
 */
@Singleton
class ReconnectController @Inject constructor(
    private val sessionEngine: SshSessionEngine,
    private val backoffPolicy: ReconnectBackoffPolicy,
    private val reconnectTrigger: ReconnectTrigger
) {

    private val _state = MutableStateFlow<ReconnectState>(ReconnectState.IDLE)
    val state: StateFlow<ReconnectState> = _state.asStateFlow()

    private var reconnectScope: CoroutineScope? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    /**
     * Starts listening for reconnection triggers.
     */
    fun startListening(scope: CoroutineScope) {
        reconnectScope = scope
        reconnectJob = scope.launch {
            reconnectTrigger.triggerFlow.collect { cause ->
                handleReconnectTrigger(cause)
            }
        }
    }

    /**
     * Stops listening for reconnection triggers.
     */
    fun stopListening() {
        reconnectJob?.cancel()
        reconnectScope = null
        reconnectJob = null
    }

    /**
     * Manually triggers a reconnection attempt.
     */
    fun triggerReconnect(cause: ReconnectCause = ReconnectCause.ConnectionLost) {
        reconnectTrigger.trigger(cause)
    }

    /**
     * Resets the reconnection state.
     */
    fun reset() {
        reconnectAttempts = 0
        _state.value = ReconnectState.IDLE
        backoffPolicy.reset()
    }

    private suspend fun handleReconnectTrigger(cause: ReconnectCause) {
        if (sessionEngine.state.value == SshSessionState.CONNECTED) {
            // Already connected, no need to reconnect
            return
        }

        if (!backoffPolicy.canRetry(reconnectAttempts)) {
            _state.value = ReconnectState.FAILED("Max reconnection attempts reached")
            return
        }

        _state.value = ReconnectState.RECONNECTING(reconnectAttempts + 1, cause)

        // Wait for backoff period
        val canContinue = backoffPolicy.waitForBackoff(reconnectAttempts)
        if (!canContinue) {
            _state.value = ReconnectState.FAILED("Max reconnection attempts reached")
            return
        }

        // Attempt reconnection
        reconnectAttempts++
        // Note: Actual reconnection logic would be handled by the session use case
        // This is a placeholder for demonstration
    }
}

/**
 * State of the reconnection process.
 */
sealed class ReconnectState {
    data object IDLE : ReconnectState()
    data class RECONNECTING(val attempt: Int, val cause: ReconnectCause) : ReconnectState()
    data class FAILED(val reason: String) : ReconnectState()
}
