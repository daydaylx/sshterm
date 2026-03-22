package com.example.privatessh.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for managing active SSH terminal sessions.
 * Tracks session state across the application.
 */
@Singleton
class SessionRegistry @Inject constructor() {

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _sessionCount = MutableStateFlow(0)
    val sessionCount: StateFlow<Int> = _sessionCount.asStateFlow()

    /**
     * Registers a new active session.
     */
    fun registerSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _sessionCount.value += 1
    }

    /**
     * Unregisters a session.
     */
    fun unregisterSession(sessionId: String) {
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = null
        }
        _sessionCount.value = maxOf(0, _sessionCount.value - 1)
    }

    /**
     * Returns true if a session is currently active.
     */
    fun hasActiveSession(): Boolean {
        return _activeSessionId.value != null
    }

    /**
     * Returns the current active session ID.
     */
    fun getActiveSessionId(): String? {
        return _activeSessionId.value
    }

    /**
     * Clears all sessions.
     */
    fun clearAll() {
        _activeSessionId.value = null
        _sessionCount.value = 0
    }
}
