package com.dlx.sshterm.domain.model

/**
 * Snapshot of a session state for persistence.
 */
data class SessionSnapshot(
    val sessionId: String,
    val hostId: String,
    val status: SessionStatus,
    val startTime: Long = System.currentTimeMillis(),
    val lastActivityTime: Long = System.currentTimeMillis(),
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val errorMessage: String? = null
) {
    /**
     * Returns the duration of the session in milliseconds.
     */
    fun getDuration(): Long = lastActivityTime - startTime

    /**
     * Returns whether the session is currently active.
     */
    fun isActive(): Boolean = status == SessionStatus.CONNECTED ||
        status == SessionStatus.GRACE_PERIOD

    /**
     * Returns whether the session has an error.
     */
    fun hasError(): Boolean = status == SessionStatus.ERROR &&
        errorMessage != null
}
