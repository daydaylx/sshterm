package com.example.privatessh.terminal

import androidx.compose.runtime.Stable

/**
 * Render state for terminal display in Compose.
 */
@Stable
data class TerminalRendererState(
    val lines: List<TerminalLine> = emptyList(),
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val columns: Int = 80,
    val rows: Int = 24,
    val scrollTop: Int = 0,
    val scrollBottom: Int = 24,
    val isCursorVisible: Boolean = true,
    val fontFamily: String = "monospace",
    val fontSize: Float = 14f
) {
    /**
     * Returns the visible lines for rendering.
     */
    fun getVisibleLines(): List<TerminalLine> {
        return lines.drop(scrollTop).take(rows)
    }

    /**
     * Returns true if the cursor is within the visible area.
     */
    fun isCursorInViewport(): Boolean {
        return cursorRow in scrollTop until scrollBottom &&
               isCursorVisible
    }

    /**
     * Creates a new state with updated dimensions.
     */
    fun withDimensions(columns: Int, rows: Int): TerminalRendererState {
        return copy(
            columns = columns,
            rows = rows,
            scrollBottom = scrollTop + rows
        )
    }

    /**
     * Creates a new state with the cursor position updated.
     */
    fun withCursor(row: Int, col: Int): TerminalRendererState {
        return copy(cursorRow = row, cursorCol = col)
    }

    /**
     * Creates a new state with scroll position updated.
     */
    fun withScroll(top: Int, bottom: Int): TerminalRendererState {
        return copy(scrollTop = top, scrollBottom = bottom)
    }

    companion object {
        /**
         * Creates a default terminal state.
         */
        fun default(): TerminalRendererState {
            return TerminalRendererState(
                columns = 80,
                rows = 24,
                scrollTop = 0,
                scrollBottom = 24
            )
        }
    }
}
