package com.example.privatessh.presentation.settings

import androidx.compose.runtime.Stable

/**
 * UI State for settings screen.
 */
@Stable
data class SettingsUiState(
    val gracePeriodMinutes: Int = 10,
    val autoReconnect: Boolean = true,
    val tmuxAutoAttach: Boolean = false,
    val tmuxSessionName: String = "",
    val terminalFontSize: Float = 14f,
    val terminalScrollbackSize: Int = 2000,
    val batteryOptimizationDisabled: Boolean = false,
    val tailscaleHostTypeDetection: Boolean = true,
    val keepScreenOn: Boolean = true,
    val biometricAuthEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Returns true if settings are currently being saved.
     */
    val isSaving: Boolean
        get() = isLoading

    /**
     * Returns true if there's an error.
     */
    val hasError: Boolean
        get() = error != null

    companion object {
        /**
         * Creates default settings state.
         */
        fun default(): SettingsUiState {
            return SettingsUiState()
        }
    }
}
