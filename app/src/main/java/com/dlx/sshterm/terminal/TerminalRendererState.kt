package com.dlx.sshterm.terminal

import androidx.compose.runtime.Stable

/**
 * Immutable snapshot consumed by the terminal UI.
 */
@Stable
data class TerminalRendererState(
    val renderRows: List<TerminalRenderRow> = List(DEFAULT_ROWS) { TerminalRenderRow.blank(DEFAULT_COLUMNS) },
    val columns: Int = DEFAULT_COLUMNS,
    val screenRows: Int = DEFAULT_ROWS,
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val isCursorVisible: Boolean = true,
    val isAlternateScreen: Boolean = false,
    val scrollbackSize: Int = 0,
    val isApplicationCursorKeys: Boolean = false,
    val isAutoWrapEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_COLUMNS = 80
        const val DEFAULT_ROWS = 24

        fun default(): TerminalRendererState = TerminalRendererState()
    }
}
