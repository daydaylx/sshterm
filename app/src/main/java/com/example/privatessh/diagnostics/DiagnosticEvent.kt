package com.example.privatessh.diagnostics

enum class DiagnosticLevel {
    INFO,
    WARN,
    ERROR
}

enum class DiagnosticCategory {
    CONNECTION,
    AUTH,
    HOST_KEY,
    SHELL,
    KEEPALIVE,
    SERVICE,
    UI
}

data class DiagnosticEvent(
    val id: Long,
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: DiagnosticLevel,
    val category: DiagnosticCategory,
    val title: String,
    val detail: String? = null,
    val sessionId: String? = null,
    val hostId: String? = null,
    val hostName: String? = null
) {
    fun matches(sessionId: String?, hostId: String?): Boolean {
        if (sessionId == null && hostId == null) {
            return true
        }

        return (sessionId != null && this.sessionId == sessionId) ||
            (hostId != null && this.hostId == hostId)
    }
}
