package com.dlx.sshterm.service

import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.ssh.SessionShellMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class ActiveSession(
    val sessionId: String,
    val hostId: String,
    val hostName: String,
    val authType: AuthType,
    val reconnectAllowed: Boolean,
    val passwordCached: Boolean,
    val privateKeyAvailable: Boolean,
    val shellMode: SessionShellMode = SessionShellMode.SHELL,
    val shellStatus: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionLifecycleState {
    IDLE,
    CONNECTED,
    DISCONNECTING,
    GRACE,
    RECONNECTING,
    FAILED
}

data class SessionRuntimeState(
    val lifecycleState: SessionLifecycleState = SessionLifecycleState.IDLE,
    val activeSession: ActiveSession? = null,
    val sessionCount: Int = 0,
    val graceMinutesRemaining: Int = 0,
    val statusMessage: String? = null
) {
    val canReconnect: Boolean
        get() = activeSession?.reconnectAllowed == true
}

/**
 * Registry for the single active SSH session and its foreground-service lifecycle.
 */
@Singleton
class SessionRegistry @Inject constructor() {

    private val _runtimeState = MutableStateFlow(SessionRuntimeState())
    val runtimeState: StateFlow<SessionRuntimeState> = _runtimeState.asStateFlow()

    fun registerSession(session: ActiveSession) {
        _runtimeState.value = SessionRuntimeState(
            lifecycleState = SessionLifecycleState.CONNECTED,
            activeSession = session,
            sessionCount = 1,
            graceMinutesRemaining = 0,
            statusMessage = session.shellStatus
        )
    }

    fun updateSession(session: ActiveSession) {
        _runtimeState.update { it.copy(
            activeSession = session,
            sessionCount = 1,
            statusMessage = session.shellStatus
        ) }
    }

    fun markGracePeriod(minutesRemaining: Int) {
        _runtimeState.update { it.copy(
            lifecycleState = SessionLifecycleState.GRACE,
            sessionCount = if (it.activeSession != null) 1 else 0,
            graceMinutesRemaining = minutesRemaining,
            statusMessage = null
        ) }
    }

    fun markReconnecting(reason: String?) {
        _runtimeState.update { it.copy(
            lifecycleState = SessionLifecycleState.RECONNECTING,
            sessionCount = if (it.activeSession != null) 1 else 0,
            graceMinutesRemaining = 0,
            statusMessage = reason
        ) }
    }

    fun markDisconnecting() {
        _runtimeState.update { it.copy(
            lifecycleState = SessionLifecycleState.DISCONNECTING,
            sessionCount = if (it.activeSession != null) 1 else 0,
            graceMinutesRemaining = 0,
            statusMessage = null
        ) }
    }

    fun markFailed(reason: String?) {
        _runtimeState.update { it.copy(
            lifecycleState = SessionLifecycleState.FAILED,
            sessionCount = if (it.activeSession != null) 1 else 0,
            graceMinutesRemaining = 0,
            statusMessage = reason
        ) }
    }

    fun markConnected(statusMessage: String? = null) {
        _runtimeState.update { it.copy(
            lifecycleState = SessionLifecycleState.CONNECTED,
            sessionCount = if (it.activeSession != null) 1 else 0,
            graceMinutesRemaining = 0,
            statusMessage = statusMessage
        ) }
    }

    fun hasActiveSession(): Boolean = _runtimeState.value.activeSession != null

    fun getActiveSession(): ActiveSession? = _runtimeState.value.activeSession

    fun getActiveSessionId(): String? = _runtimeState.value.activeSession?.sessionId

    fun clearAll() {
        _runtimeState.value = SessionRuntimeState()
    }
}
