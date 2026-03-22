package com.example.privatessh.terminal

/**
 * Ring buffer for terminal screen content with scrollback support.
 */
class TerminalBuffer(
    private val maxLines: Int = 1000,
    private val scrollbackLines: Int = 10000
) {
    private val lines = mutableListOf<TerminalLine>()
    private var cursorRow = 0
    private var cursorCol = 0

    /**
     * Adds a new line to the buffer.
     */
    fun addLine(line: TerminalLine) {
        if (cursorRow < lines.size) {
            // Overwrite existing line
            lines[cursorRow] = line
        } else {
            // Add new line
            lines.add(line)
        }

        cursorRow++
        cursorCol = 0

        // Trim to max lines if needed
        if (lines.size > scrollbackLines) {
            val removeCount = lines.size - scrollbackLines
            repeat(removeCount) {
                if (lines.isNotEmpty()) {
                    lines.removeAt(0)
                }
            }
            cursorRow = cursorRow.coerceAtLeast(0)
        }
    }

    /**
     * Returns the line at the specified row.
     */
    fun getLine(row: Int): TerminalLine? {
        return lines.getOrNull(row)
    }

    /**
     * Returns all visible lines (current screen).
     */
    fun getVisibleLines(startRow: Int, count: Int): List<TerminalLine> {
        val endRow = minOf(startRow + count, lines.size)
        return lines.subList(startRow, endRow)
    }

    /**
     * Returns the total number of lines in the buffer.
     */
    fun getLineCount(): Int {
        return lines.size
    }

    /**
     * Clears all lines from the buffer.
     */
    fun clear() {
        lines.clear()
        cursorRow = 0
        cursorCol = 0
    }

    /**
     * Clears the current visible screen area.
     */
    fun clearScreen(rows: Int) {
        val startRow = cursorRow - cursorRow.coerceAtMost(0)
        for (i in 0 until rows) {
            if (startRow + i < lines.size) {
                lines[startRow + i] = TerminalLine.empty()
            }
        }
    }

    /**
     * Inserts a character at the current cursor position.
     */
    fun insertChar(char: Char, attribute: TerminalTextAttribute) {
        if (cursorRow >= lines.size) {
            // Add new lines if needed
            while (lines.size <= cursorRow) {
                lines.add(TerminalLine.empty())
            }
        }

        val currentLine = lines[cursorRow]
        val segment = TextSegment(char.toString(), attribute)

        // Insert character at cursor column
        val updatedSegments = currentLine.segments.toMutableList()
        var colOffset = 0

        for (i in updatedSegments.indices) {
            val seg = updatedSegments[i]
            val segLength = seg.length()

            if (cursorCol <= colOffset + segLength) {
                // Found the segment to insert into
                if (cursorCol == colOffset) {
                    // Insert before this segment
                    updatedSegments.add(i, segment)
                } else {
                    // Split and insert
                    val splitPos = cursorCol - colOffset
                    val (before, after) = seg.splitAt(splitPos)
                    updatedSegments[i] = before
                    updatedSegments.add(i + 1, segment)
                    if (after != null) {
                        updatedSegments.add(i + 2, after)
                    }
                }
                break
            }

            colOffset += segLength
        }

        lines[cursorRow] = currentLine.copy(segments = updatedSegments)
        cursorCol++
    }

    /**
     * Moves the cursor to the specified position.
     */
    fun moveCursor(row: Int, col: Int) {
        cursorRow = row.coerceIn(0, lines.size)
        cursorCol = col.coerceAtLeast(0)
    }

    /**
     * Returns the current cursor position.
     */
    fun getCursor(): Pair<Int, Int> {
        return cursorRow to cursorCol
    }

    /**
     * Scrolls the buffer up by the specified number of lines.
     */
    fun scrollUp(lineCount: Int): Int {
        val scrollAmount = lineCount.coerceAtMost(lines.size)
        // Remove lines from the top
        repeat(scrollAmount) {
            if (lines.isNotEmpty()) {
                lines.removeAt(0)
            }
        }
        cursorRow = cursorRow.coerceAtLeast(0)
        return scrollAmount
    }

    /**
     * Returns the scrollback history.
     */
    fun getScrollback(): List<TerminalLine> {
        return lines.toList()
    }
}
