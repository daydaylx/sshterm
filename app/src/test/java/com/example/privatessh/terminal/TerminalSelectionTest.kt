package com.example.privatessh.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalSelectionTest {

    @Test
    fun extractSelectionText_returns_single_row_slice() {
        val rendererState = rendererStateOf("hello world")
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 0),
            focus = TerminalCellPosition(row = 0, column = 4)
        )

        assertEquals("hello", rendererState.extractSelectionText(selection))
    }

    @Test
    fun extractSelectionText_returns_multi_row_text_with_newlines() {
        val rendererState = rendererStateOf(
            "hello world",
            "middle row",
            "tail"
        )
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 6),
            focus = TerminalCellPosition(row = 2, column = 1)
        )

        assertEquals("world\nmiddle row\nta", rendererState.extractSelectionText(selection))
    }

    @Test
    fun contains_handles_reversed_selection_coordinates() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 2, column = 3),
            focus = TerminalCellPosition(row = 1, column = 1)
        )

        assertTrue(selection.contains(1, 1))
        assertTrue(selection.contains(1, 5))
        assertTrue(selection.contains(2, 2))
    }

    private fun rendererStateOf(vararg rows: String): TerminalRendererState {
        val width = rows.maxOfOrNull { it.length } ?: 1
        return TerminalRendererState(
            renderRows = rows.map { row ->
                TerminalRenderRow(
                    cells = row.padEnd(width).map { char ->
                        TerminalCell(char = char)
                    }
                )
            },
            columns = width,
            screenRows = rows.size
        )
    }
}
