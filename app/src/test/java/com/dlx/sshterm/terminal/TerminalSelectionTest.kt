package com.dlx.sshterm.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun contains_returnsFalseForRowBeforeSelection() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 2, column = 0),
            focus = TerminalCellPosition(row = 4, column = 5)
        )

        assertFalse(selection.contains(1, 0))
        assertFalse(selection.contains(0, 10))
    }

    @Test
    fun contains_returnsFalseForRowAfterSelection() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 2, column = 0),
            focus = TerminalCellPosition(row = 4, column = 5)
        )

        assertFalse(selection.contains(5, 0))
        assertFalse(selection.contains(10, 3))
    }

    @Test
    fun contains_returnsFalseForColumnBeforeStartOnFirstRow() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 1, column = 5),
            focus = TerminalCellPosition(row = 3, column = 8)
        )

        assertFalse(selection.contains(1, 4))
        assertFalse(selection.contains(1, 0))
    }

    @Test
    fun contains_returnsFalseForColumnAfterEndOnLastRow() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 1, column = 0),
            focus = TerminalCellPosition(row = 3, column = 5)
        )

        assertFalse(selection.contains(3, 6))
        assertFalse(selection.contains(3, 100))
    }

    @Test
    fun contains_returnsTrueForAllColumnsOnMiddleRows() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 3),
            focus = TerminalCellPosition(row = 2, column = 3)
        )

        assertTrue(selection.contains(1, 0))
        assertTrue(selection.contains(1, 79))
    }

    @Test
    fun normalized_anchorBeforeFocus_returnsAnchorFirst() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 0),
            focus = TerminalCellPosition(row = 1, column = 5)
        )
        val (start, end) = selection.normalized()

        assertEquals(TerminalCellPosition(0, 0), start)
        assertEquals(TerminalCellPosition(1, 5), end)
    }

    @Test
    fun normalized_focusBeforeAnchor_returnsFocusFirst() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 3, column = 7),
            focus = TerminalCellPosition(row = 1, column = 2)
        )
        val (start, end) = selection.normalized()

        assertEquals(TerminalCellPosition(1, 2), start)
        assertEquals(TerminalCellPosition(3, 7), end)
    }

    @Test
    fun normalized_sameRow_anchorAfterFocusColumn_returnsFocusFirst() {
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 2, column = 8),
            focus = TerminalCellPosition(row = 2, column = 3)
        )
        val (start, end) = selection.normalized()

        assertEquals(TerminalCellPosition(2, 3), start)
        assertEquals(TerminalCellPosition(2, 8), end)
    }

    @Test
    fun extractSelectionText_withSingleCharSelection() {
        val rendererState = rendererStateOf("hello")
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 2),
            focus = TerminalCellPosition(row = 0, column = 2)
        )

        assertEquals("l", rendererState.extractSelectionText(selection))
    }

    @Test
    fun extractSelectionText_trims_trailingSpacesFromEachLine() {
        val rendererState = rendererStateOf("hi   ", "world")
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 0),
            focus = TerminalCellPosition(row = 1, column = 4)
        )

        val result = rendererState.extractSelectionText(selection)
        assertTrue(result.startsWith("hi"))
        assertFalse(result.lines()[0].endsWith(" "))
    }

    @Test
    fun extractSelectionText_outOfBoundsRows_clampToValidRange() {
        val rendererState = rendererStateOf("line0", "line1")
        val selection = TerminalSelection(
            anchor = TerminalCellPosition(row = 0, column = 0),
            focus = TerminalCellPosition(row = 99, column = 4) // beyond last row
        )

        // Must not throw; content from row 0 and row 1 is returned
        val result = rendererState.extractSelectionText(selection)
        assertTrue(result.contains("line0") || result.contains("line1"))
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
