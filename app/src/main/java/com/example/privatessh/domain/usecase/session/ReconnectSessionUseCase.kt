package com.example.privatessh.domain.usecase.session

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.ssh.SshSessionEngine
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import com.example.privatessh.ssh.reconnect.ReconnectController
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Reconnects to an SSH session.
 */
@ViewModelScoped
class ReconnectSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine,
    private val stopSessionUseCase: StopSessionUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val reconnectController: ReconnectController
) {

    suspend operator fun invoke(
        hostProfile: HostProfile,
        onHostKeyUnknown: (algorithm: String, fingerprint: String) -> HostKeyDecision
    ): Boolean {
        // First, stop the current session if any
        stopSessionUseCase()

        // Trigger reconnection state
        reconnectController.triggerReconnect()

        // Then start a new session
        return startSessionUseCase(
            hostProfile = hostProfile,
            onHostKeyUnknown = onHostKeyUnknown
        )
    }

    /**
     * Triggers a manual reconnection attempt.
     */
    fun triggerManualReconnect() {
        reconnectController.triggerReconnect()
    }

    /**
     * Resets the reconnection state.
     */
    fun resetReconnectState() {
        reconnectController.reset()
    }
}
