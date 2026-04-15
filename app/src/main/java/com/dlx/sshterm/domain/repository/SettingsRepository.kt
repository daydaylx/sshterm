package com.dlx.sshterm.domain.repository

import com.dlx.sshterm.domain.model.SessionPolicy
import com.dlx.sshterm.domain.model.TerminalMetrics
import kotlinx.coroutines.flow.Flow

/**
 * Repository for app settings.
 */
interface SettingsRepository {
    /**
     * Observes the session policy.
     */
    fun observeSessionPolicy(): Flow<SessionPolicy>

    /**
     * Observes the terminal metrics.
     */
    fun observeTerminalMetrics(): Flow<TerminalMetrics>

    /**
     * Observes whether battery optimization is disabled.
     */
    fun observeBatteryOptimizationDisabled(): Flow<Boolean>

    /**
     * Observes whether Tailscale target detection is enabled.
     */
    fun observeTailscaleHostTypeDetection(): Flow<Boolean>

    /**
     * Observes whether the screen should stay on during an active session.
     */
    fun observeKeepScreenOn(): Flow<Boolean>

    /**
     * Observes whether biometric authentication is required before connecting.
     */
    fun observeBiometricAuthEnabled(): Flow<Boolean>

    /**
     * Observes whether terminal edge-to-edge display is enabled.
     */
    fun observeTerminalEdgeToEdge(): Flow<Boolean>

    /**
     * Sets the grace period duration in minutes.
     */
    suspend fun setGracePeriod(minutes: Int)

    /**
     * Sets whether auto-reconnect is enabled.
     */
    suspend fun setAutoReconnect(enabled: Boolean)

    /**
     * Sets whether tmux auto-attach is enabled.
     */
    suspend fun setTmuxAutoAttach(enabled: Boolean)

    /**
     * Sets the tmux session name.
     */
    suspend fun setTmuxSessionName(name: String?)

    /**
     * Sets the terminal font size.
     */
    suspend fun setTerminalFontSize(size: Float)

    /**
     * Sets the terminal scrollback buffer size in lines.
     */
    suspend fun setScrollbackSize(lines: Int)

    /**
     * Sets whether battery optimization is disabled.
     */
    suspend fun setBatteryOptimizationDisabled(disabled: Boolean)

    /**
     * Sets whether Tailscale target detection is enabled.
     */
    suspend fun setTailscaleHostTypeDetection(enabled: Boolean)

    /**
     * Sets whether the screen should stay on during active sessions.
     */
    suspend fun setKeepScreenOn(enabled: Boolean)

    /**
     * Sets whether biometric authentication is required before connecting.
     */
    suspend fun setBiometricAuthEnabled(enabled: Boolean)

    /**
     * Sets whether terminal edge-to-edge display is enabled.
     */
    suspend fun setTerminalEdgeToEdge(enabled: Boolean)

    /**
     * Gets the current session policy.
     */
    suspend fun getSessionPolicy(): SessionPolicy

    /**
     * Gets the current terminal metrics.
     */
    suspend fun getTerminalMetrics(): TerminalMetrics
}
