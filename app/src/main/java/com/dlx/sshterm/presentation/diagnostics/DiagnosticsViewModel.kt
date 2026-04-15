package com.dlx.sshterm.presentation.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlx.sshterm.diagnostics.DiagnosticEvent
import com.dlx.sshterm.diagnostics.SessionDiagnosticsStore
import com.dlx.sshterm.service.SessionRegistry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val diagnosticsStore: SessionDiagnosticsStore,
    private val sessionRegistry: SessionRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState = _uiState.asStateFlow()

    private var latestEvents: List<DiagnosticEvent> = emptyList()
    private var selectedSessionId: String? = null
    private var selectedHostId: String? = null
    private var selectedHostName: String = ""

    init {
        viewModelScope.launch {
            sessionRegistry.runtimeState.collect { runtimeState ->
                runtimeState.activeSession?.let { activeSession ->
                    selectedSessionId = activeSession.sessionId
                    selectedHostId = activeSession.hostId
                    selectedHostName = activeSession.hostName
                }

                updateUiState(
                    lifecycleState = runtimeState.lifecycleState,
                    statusMessage = runtimeState.statusMessage
                )
            }
        }

        viewModelScope.launch {
            diagnosticsStore.events.collect { events ->
                latestEvents = events
                updateUiState(
                    lifecycleState = sessionRegistry.runtimeState.value.lifecycleState,
                    statusMessage = sessionRegistry.runtimeState.value.statusMessage
                )
            }
        }
    }

    fun clearVisible() {
        val sessionId = selectedSessionId
        val hostId = selectedHostId
        when {
            !sessionId.isNullOrBlank() -> diagnosticsStore.clearSession(sessionId)
            !hostId.isNullOrBlank() -> diagnosticsStore.clearSession(hostId)
            else -> diagnosticsStore.clearAll()
        }
    }

    private fun updateUiState(
        lifecycleState: com.dlx.sshterm.service.SessionLifecycleState,
        statusMessage: String?
    ) {
        val visibleEvents = latestEvents
            .filter { it.matches(selectedSessionId, selectedHostId) }
            .ifEmpty {
                if (selectedSessionId == null && selectedHostId == null) latestEvents else emptyList()
            }
            .sortedByDescending { it.id }

        _uiState.value = DiagnosticsUiState(
            hostName = selectedHostName,
            sessionId = selectedSessionId,
            lifecycleState = lifecycleState,
            statusMessage = statusMessage,
            events = visibleEvents,
            isShowingLastSession = sessionRegistry.getActiveSession() == null &&
                selectedSessionId != null &&
                visibleEvents.isNotEmpty()
        )
    }
}
