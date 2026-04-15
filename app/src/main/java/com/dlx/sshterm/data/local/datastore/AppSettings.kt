package com.dlx.sshterm.data.local.datastore

import com.dlx.sshterm.domain.model.SessionPolicy
import com.dlx.sshterm.domain.model.TerminalMetrics

/**
 * App settings data class.
 */
data class AppSettings(
    val gracePeriodMinutes: Int = DEFAULT_GRACE_PERIOD,
    val autoReconnect: Boolean = DEFAULT_AUTO_RECONNECT,
    val tmuxAutoAttach: Boolean = DEFAULT_TMUX_AUTO_ATTACH,
    val tmuxSessionName: String? = null,
    val terminalFontSize: Float = DEFAULT_FONT_SIZE,
    val terminalColumns: Int = DEFAULT_COLUMNS,
    val terminalRows: Int = DEFAULT_ROWS,
    val terminalScrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE,
    val batteryOptimizationDisabled: Boolean = DEFAULT_BATTERY_OPTIMIZATION_DISABLED,
    val tailscaleHostTypeDetection: Boolean = DEFAULT_TAILSCALE_HOST_TYPE_DETECTION,
    val keepScreenOn: Boolean = DEFAULT_KEEP_SCREEN_ON,
    val biometricAuthEnabled: Boolean = DEFAULT_BIOMETRIC_AUTH_ENABLED,
    val terminalEdgeToEdge: Boolean = DEFAULT_TERMINAL_EDGE_TO_EDGE
) {
    companion object {
        const val DEFAULT_GRACE_PERIOD = 10
        const val DEFAULT_AUTO_RECONNECT = true
        const val DEFAULT_TMUX_AUTO_ATTACH = false
        const val DEFAULT_FONT_SIZE = 14f
        const val DEFAULT_COLUMNS = 80
        const val DEFAULT_ROWS = 24
        const val DEFAULT_SCROLLBACK_SIZE = 2000
        const val DEFAULT_BATTERY_OPTIMIZATION_DISABLED = false
        const val DEFAULT_TAILSCALE_HOST_TYPE_DETECTION = true
        const val DEFAULT_KEEP_SCREEN_ON = true
        const val DEFAULT_BIOMETRIC_AUTH_ENABLED = false
        const val DEFAULT_TERMINAL_EDGE_TO_EDGE = false
    }

    /**
     * Converts to SessionPolicy domain model.
     */
    fun toSessionPolicy(): SessionPolicy = SessionPolicy(
        gracePeriodMinutes = gracePeriodMinutes,
        autoReconnect = autoReconnect,
        tmuxAutoAttach = tmuxAutoAttach,
        tmuxSessionName = tmuxSessionName
    )

    /**
     * Converts to TerminalMetrics domain model.
     */
    fun toTerminalMetrics(): TerminalMetrics = TerminalMetrics(
        columns = terminalColumns,
        rows = terminalRows,
        fontSize = terminalFontSize,
        scrollbackSize = terminalScrollbackSize
    )
}
