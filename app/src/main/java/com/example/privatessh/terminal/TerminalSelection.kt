package com.example.privatessh.terminal

data class TerminalCellPosition(
    val row: Int,
    val column: Int
)

data class TerminalSelection(
    val anchor: TerminalCellPosition,
    val focus: TerminalCellPosition = anchor
) {
    fun normalized(): Pair<TerminalCellPosition, TerminalCellPosition> {
        return if (
            anchor.row < focus.row ||
            (anchor.row == focus.row && anchor.column <= focus.column)
        ) {
            anchor to focus
        } else {
            focus to anchor
        }
    }

    fun contains(row: Int, column: Int): Boolean {
        val (start, end) = normalized()
        if (row !in start.row..end.row) {
            return false
        }

        return when (row) {
            start.row -> {
                val startColumn = start.column
                val endColumn = if (start.row == end.row) end.column else Int.MAX_VALUE
                column in startColumn..endColumn
            }

            end.row -> column <= end.column
            else -> true
        }
    }
}

fun TerminalRendererState.extractSelectionText(selection: TerminalSelection): String {
    val (start, end) = selection.normalized()
    if (renderRows.isEmpty()) {
        return ""
    }

    val safeStartRow = start.row.coerceIn(0, renderRows.lastIndex)
    val safeEndRow = end.row.coerceIn(safeStartRow, renderRows.lastIndex)
    val lines = mutableListOf<String>()

    for (rowIndex in safeStartRow..safeEndRow) {
        val row = renderRows[rowIndex]
        if (row.cells.isEmpty()) {
            lines += ""
            continue
        }

        val startColumn = if (rowIndex == safeStartRow) {
            start.column.coerceIn(0, row.cells.lastIndex)
        } else {
            0
        }
        val endColumn = if (rowIndex == safeEndRow) {
            end.column.coerceIn(startColumn, row.cells.lastIndex)
        } else {
            row.cells.lastIndex
        }

        val text = buildString {
            for (column in startColumn..endColumn) {
                append(row.cells[column].char)
            }
        }.trimEnd()

        lines += text
    }

    return lines.joinToString(separator = "\n")
}
