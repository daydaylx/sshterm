package com.example.privatessh.core.constants

/**
 * Terminal-specific defaults.
 */
object TerminalDefaults {
    // Terminal capabilities
    const val TERMINAL_TYPE = "xterm-256color"
    const val SUPPORTS_COLORS = true
    const val SUPPORTS_ALTERNATE_SCREEN = true
    const val SUPPORTS_CURSOR_KEYS = true

    // Buffer sizes
    const val MAX_HISTORY_LINES = 10000
    const val MAX_OUTPUT_BUFFER_SIZE = 1024 * 1024 // 1MB

    // ANSI colors (standard 16 colors)
    val ANSI_COLORS = listOf(
        0xFF000000.toInt(), // Black
        0xFFCD0000.toInt(), // Red
        0xFF00CD00.toInt(), // Green
        0xFFCDCD00.toInt(), // Yellow
        0xFF0000EE.toInt(), // Blue
        0xFFCD00CD.toInt(), // Magenta
        0xFF00CDCD.toInt(), // Cyan
        0xFFE5E5E5.toInt(), // White
        0xFF7F7F7F.toInt(), // Bright Black (Gray)
        0xFFFF0000.toInt(), // Bright Red
        0xFF00FF00.toInt(), // Bright Green
        0xFFFFFF00.toInt(), // Bright Yellow
        0xFF5C5CFF.toInt(), // Bright Blue
        0xFFFF00FF.toInt(), // Bright Magenta
        0xFF00FFFF.toInt(), // Bright Cyan
        0xFFFFFFFF.toInt(), // Bright White
    )
}
