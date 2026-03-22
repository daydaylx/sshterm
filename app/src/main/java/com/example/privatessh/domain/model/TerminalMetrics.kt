package com.example.privatessh.domain.model

/**
 * Terminal dimensions and display settings.
 */
data class TerminalMetrics(
    val columns: Int = DEFAULT_COLUMNS,
    val rows: Int = DEFAULT_ROWS,
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val scrollbackSize: Int = DEFAULT_SCROLLBACK_SIZE
) {
    companion object {
        const val DEFAULT_COLUMNS = 80
        const val DEFAULT_ROWS = 24
        const val DEFAULT_FONT_SIZE = 14f
        const val DEFAULT_SCROLLBACK_SIZE = 2000
    }

    /**
     * Validates the metrics are within acceptable ranges.
     */
    fun isValid(): Boolean {
        return columns in 10..500 && rows in 5..200 && fontSize in 8f..32f
    }
}
