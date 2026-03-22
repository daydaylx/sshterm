package com.example.privatessh.presentation.terminal

import com.example.privatessh.ssh.SshSessionState
import com.example.privatessh.service.SessionLifecycleState
import com.example.privatessh.terminal.TerminalSelection
import com.example.privatessh.terminal.TerminalRendererState
import com.example.privatessh.terminal.input.ModifierKey
import com.example.privatessh.terminal.input.ModifierState

/**
 * UI State for terminal screen.
 */
data class TerminalUiState(
    val sessionState: SshSessionState = SshSessionState.DISCONNECTED,
    val lifecycleState: SessionLifecycleState = SessionLifecycleState.IDLE,
    val hostName: String = "",
    val rendererState: TerminalRendererState = TerminalRendererState.default(),
    val activeModifiers: Set<ModifierKey> = emptySet(),
    val modifierStates: Map<ModifierKey, ModifierState> = emptyMap(),
    val terminalColumns: Int = 80,
    val terminalRows: Int = 24,
    val terminalFontSizeSp: Float = 14f,
    val canReconnect: Boolean = false,
    val graceMinutesRemaining: Int = 0,
    val statusMessage: String? = null,
    val keepScreenOn: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isAwaitingPassword: Boolean = false,
    val hostKeyPrompt: HostKeyPrompt? = null,
    val selection: TerminalSelection? = null,
    val selectedText: String = ""
) {
    /**
     * Returns true if connected to SSH session.
     */
    val isConnected: Boolean
        get() = sessionState == SshSessionState.CONNECTED

    /**
     * Returns true if currently connecting or authenticating.
     */
    val isConnecting: Boolean
        get() = sessionState in setOf(
            SshSessionState.CONNECTING,
            SshSessionState.RECONNECTING,
            SshSessionState.AUTHENTICATING
        )

    val isReconnecting: Boolean
        get() = lifecycleState == SessionLifecycleState.RECONNECTING ||
            sessionState == SshSessionState.RECONNECTING

    /**
     * Returns true if there was an error.
     */
    val hasError: Boolean
        get() = error != null

    /**
     * Returns true if Ctrl modifier is active.
     */
    val isCtrlActive: Boolean
        get() = ModifierKey.CTRL in activeModifiers

    /**
     * Returns true if Alt modifier is active.
     */
    val isAltActive: Boolean
        get() = ModifierKey.ALT in activeModifiers

    /**
     * Returns true if Shift modifier is active.
     */
    val isShiftActive: Boolean
        get() = ModifierKey.SHIFT in activeModifiers

    /**
     * Returns the state of Ctrl modifier.
     */
    val ctrlState: ModifierState
        get() = modifierStates[ModifierKey.CTRL] ?: ModifierState.Inactive

    /**
     * Returns the state of Alt modifier.
     */
    val altState: ModifierState
        get() = modifierStates[ModifierKey.ALT] ?: ModifierState.Inactive

    /**
     * Returns the state of Shift modifier.
     */
    val shiftState: ModifierState
        get() = modifierStates[ModifierKey.SHIFT] ?: ModifierState.Inactive

    val hasSelection: Boolean
        get() = selection != null && selectedText.isNotEmpty()
}

data class HostKeyPrompt(
    val algorithm: String,
    val fingerprint: String
)
