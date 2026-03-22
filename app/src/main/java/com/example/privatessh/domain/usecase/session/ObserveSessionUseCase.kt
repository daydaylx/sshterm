package com.example.privatessh.domain.usecase.session

import com.example.privatessh.ssh.SshSessionEngine
import com.example.privatessh.ssh.SshSessionState
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.terminal.TerminalRendererState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Observes the SSH session state.
 */
@ViewModelScoped
class ObserveSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine
) {

    /**
     * Returns the session state flow.
     */
    operator fun invoke(): StateFlow<SshSessionState> {
        return sessionEngine.state
    }

    /**
     * Returns the error flow.
     */
    fun observeErrors(): StateFlow<String?> {
        return sessionEngine.error
    }

    fun observeTerminalRendererState(): StateFlow<TerminalRendererState> {
        return sessionEngine.terminalRendererState
    }

    fun observeCurrentHost(): StateFlow<HostProfile?> {
        return sessionEngine.currentHost
    }

    /**
     * Returns the current session state.
     */
    fun getCurrentState(): SshSessionState {
        return sessionEngine.state.value
    }

    /**
     * Returns the current error if any.
     */
    fun getCurrentError(): String? {
        return sessionEngine.error.value
    }

    fun getCurrentHost(): HostProfile? {
        return sessionEngine.currentHost.value
    }
}
