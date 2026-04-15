package com.dlx.sshterm.core.constants

/**
 * Default values for the application.
 */
object Defaults {
    // Connection
    const val DEFAULT_PORT = 22
    const val CONNECTION_TIMEOUT_MS = 15000L
    const val READ_TIMEOUT_MS = 30000L

    // Grace Period
    const val GRACE_PERIOD_MINUTES_DEFAULT = 10
    const val GRACE_PERIOD_MINUTES_EXTENDED = 30

    // Terminal
    const val TERMINAL_COLUMNS_DEFAULT = 80
    const val TERMINAL_ROWS_DEFAULT = 24
    const val TERMINAL_FONT_SIZE_SP = 14f
    const val TERMINAL_SCROLLBACK_LINES = 10000

    // Reconnect
    const val RECONNECT_MAX_ATTEMPTS = 5
    const val RECONNECT_INITIAL_DELAY_MS = 1000L
    const val RECONNECT_MAX_DELAY_MS = 30000L

    // Keepalive
    const val KEEPALIVE_INTERVAL_MS = 30000L
    const val HEARTBEAT_TIMEOUT_MS = 60000L
}
