package com.example.privatessh.domain.repository

import com.example.privatessh.domain.model.SessionPolicy
import com.example.privatessh.domain.model.TerminalMetrics
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
     * Gets the current session policy.
     */
    suspend fun getSessionPolicy(): SessionPolicy

    /**
     * Gets the current terminal metrics.
     */
    suspend fun getTerminalMetrics(): TerminalMetrics
}
