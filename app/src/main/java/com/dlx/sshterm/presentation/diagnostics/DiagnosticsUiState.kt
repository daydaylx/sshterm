package com.dlx.sshterm.presentation.diagnostics

import androidx.compose.runtime.Stable
import com.dlx.sshterm.diagnostics.DiagnosticEvent
import com.dlx.sshterm.service.SessionLifecycleState

@Stable
data class DiagnosticsUiState(
    val hostName: String = "",
    val sessionId: String? = null,
    val lifecycleState: SessionLifecycleState = SessionLifecycleState.IDLE,
    val statusMessage: String? = null,
    val events: List<DiagnosticEvent> = emptyList(),
    val isShowingLastSession: Boolean = false
) {
    val hasEvents: Boolean
        get() = events.isNotEmpty()
}
