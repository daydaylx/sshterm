package com.example.privatessh.terminal

/**
 * Mutable terminal screen and scrollback storage.
 */
class TerminalBuffer(
    initialColumns: Int = 80,
    initialRows: Int = 24,
    private var scrollbackLimit: Int = 2000
) {
    var columns: Int = initialColumns
        private set

    var screenRows: Int = initialRows
        private set

    private var primaryScreen = blankScreen(columns, screenRows)
    private var alternateScreen: MutableList<MutableList<TerminalCell>>? = null
    private var savedPrimaryScreen: MutableList<MutableList<TerminalCell>>? = null
    private var savedPrimaryState: TerminalState? = null
    private val scrollback = mutableListOf<List<TerminalCell>>()

    private var state = TerminalState(scrollBottom = screenRows - 1)
    private var isAlternateScreen = false

    fun setScrollbackLimit(limit: Int) {
        scrollbackLimit = limit.coerceAtLeast(100)
    }

    fun reset(columns: Int = this.columns, rows: Int = screenRows) {
        this.columns = columns.coerceAtLeast(10)
        screenRows = rows.coerceAtLeast(5)
        primaryScreen = blankScreen(this.columns, screenRows)
        alternateScreen = null
        savedPrimaryScreen = null
        savedPrimaryState = null
        scrollback.clear()
        isAlternateScreen = false
        state = TerminalState(scrollBottom = screenRows - 1)
    }

    fun resize(columns: Int, rows: Int) {
        val newColumns = columns.coerceAtLeast(10)
        val newRows = rows.coerceAtLeast(5)

        primaryScreen = resizeScreen(primaryScreen, newColumns, newRows)
        alternateScreen = alternateScreen?.let { resizeScreen(it, newColumns, newRows) }
        savedPrimaryScreen = savedPrimaryScreen?.let { resizeScreen(it, newColumns, newRows) }

        this.columns = newColumns
        screenRows = newRows
        state = clampState(state, newColumns, newRows)
        savedPrimaryState = savedPrimaryState?.let { clampState(it, newColumns, newRows) }
    }

    fun writeText(text: String) {
        text.forEach(::putChar)
    }

    fun carriageReturn() {
        state = state.copy(cursorCol = 0)
    }

    fun lineFeed() {
        if (state.cursorRow >= state.scrollBottom) {
            scrollRegionUp(1)
        } else {
            state = state.copy(cursorRow = (state.cursorRow + 1).coerceAtMost(screenRows - 1))
        }
    }

    fun reverseIndex() {
        if (state.cursorRow <= state.scrollTop) {
            scrollRegionDown(1)
        } else {
            state = state.copy(cursorRow = (state.cursorRow - 1).coerceAtLeast(0))
        }
    }

    fun backspace() {
        state = state.copy(cursorCol = (state.cursorCol - 1).coerceAtLeast(0))
    }

    fun tab() {
        val nextStop = ((resolvedCursorCol() / 8) + 1) * 8
        state = state.copy(cursorCol = nextStop.coerceAtMost(columns - 1))
    }

    fun moveCursorTo(row: Int, col: Int) {
        state = state.copy(
            cursorRow = row.coerceIn(0, screenRows - 1),
            cursorCol = col.coerceIn(0, columns - 1)
        )
    }

    fun moveCursorRelative(rows: Int, columns: Int) {
        moveCursorTo(
            row = state.cursorRow + rows,
            col = resolvedCursorCol() + columns
        )
    }

    fun moveCursorColumn(col: Int) {
        state = state.copy(cursorCol = col.coerceIn(0, this.columns - 1))
    }

    fun moveCursorRow(row: Int) {
        state = state.copy(cursorRow = row.coerceIn(0, screenRows - 1))
    }

    fun cursorNextLine(count: Int) {
        moveCursorTo(
            row = state.cursorRow + count.coerceAtLeast(1),
            col = 0
        )
    }

    fun cursorPreviousLine(count: Int) {
        moveCursorTo(
            row = state.cursorRow - count.coerceAtLeast(1),
            col = 0
        )
    }

    fun eraseInDisplay(mode: Int) {
        when (mode) {
            0 -> {
                eraseInLine(0)
                for (row in (state.cursorRow + 1) until screenRows) {
                    clearRow(row)
                }
            }

            1 -> {
                eraseInLine(1)
                for (row in 0 until state.cursorRow) {
                    clearRow(row)
                }
            }

            2 -> {
                for (row in 0 until screenRows) {
                    clearRow(row)
                }
            }

            3 -> scrollback.clear()
        }
    }

    fun eraseInLine(mode: Int) {
        val row = currentScreen()[state.cursorRow]
        when (mode) {
            0 -> fillRange(row, resolvedCursorCol(), columns - 1)
            1 -> fillRange(row, 0, resolvedCursorCol())
            2 -> fillRange(row, 0, columns - 1)
        }
    }

    fun insertLines(count: Int) {
        if (state.cursorRow !in state.scrollTop..state.scrollBottom) {
            return
        }
        val amount = count.coerceIn(1, state.scrollBottom - state.cursorRow + 1)
        repeat(amount) {
            val screen = currentScreen()
            screen.removeAt(state.scrollBottom)
            screen.add(state.cursorRow, blankRow(columns))
        }
    }

    fun deleteLines(count: Int) {
        if (state.cursorRow !in state.scrollTop..state.scrollBottom) {
            return
        }
        val amount = count.coerceIn(1, state.scrollBottom - state.cursorRow + 1)
        repeat(amount) {
            val screen = currentScreen()
            screen.removeAt(state.cursorRow)
            screen.add(state.scrollBottom, blankRow(columns))
        }
    }

    fun deleteChars(count: Int) {
        val row = currentScreen()[state.cursorRow]
        val amount = count.coerceAtLeast(1)
        for (column in resolvedCursorCol() until this.columns) {
            val sourceIndex = column + amount
            row[column] = if (sourceIndex in 0 until this.columns) {
                row[sourceIndex]
            } else {
                TerminalCell.blank()
            }
        }
    }

    fun eraseChars(count: Int) {
        val row = currentScreen()[state.cursorRow]
        val startColumn = resolvedCursorCol()
        val endColumn = (startColumn + count.coerceAtLeast(1) - 1).coerceAtMost(columns - 1)
        fillRange(row, startColumn, endColumn)
    }

    fun scrollUp(count: Int) {
        scrollRegionUp(count.coerceAtLeast(1))
    }

    fun scrollDown(count: Int) {
        scrollRegionDown(count.coerceAtLeast(1))
    }

    fun setScrollRegion(top: Int, bottom: Int) {
        val clampedTop = top.coerceIn(0, screenRows - 1)
        val clampedBottom = bottom.coerceIn(clampedTop, screenRows - 1)
        state = state.copy(
            scrollTop = clampedTop,
            scrollBottom = clampedBottom,
            cursorRow = 0,
            cursorCol = 0
        )
    }

    fun saveCursor() {
        state = state.copy(
            savedCursorRow = state.cursorRow,
            savedCursorCol = resolvedCursorCol(),
            savedApplicationCursorKeys = state.isApplicationCursorKeys,
            savedAutoWrapEnabled = state.isAutoWrapEnabled
        )
    }

    fun restoreCursor() {
        state = state.copy(
            cursorRow = state.savedCursorRow.coerceIn(0, screenRows - 1),
            cursorCol = state.savedCursorCol.coerceIn(0, columns - 1),
            isApplicationCursorKeys = state.savedApplicationCursorKeys,
            isAutoWrapEnabled = state.savedAutoWrapEnabled
        )
    }

    fun setCursorVisible(visible: Boolean) {
        state = state.copy(isCursorVisible = visible)
    }

    fun setApplicationCursorKeys(enabled: Boolean) {
        state = state.copy(isApplicationCursorKeys = enabled)
    }

    fun setAutoWrapEnabled(enabled: Boolean) {
        state = state.copy(isAutoWrapEnabled = enabled)
    }

    fun useAlternateScreen(enabled: Boolean) {
        if (enabled && !isAlternateScreen) {
            savedPrimaryScreen = cloneScreen(primaryScreen)
            savedPrimaryState = state
            alternateScreen = blankScreen(columns, screenRows)
            isAlternateScreen = true
            state = TerminalState(
                cursorRow = 0,
                cursorCol = 0,
                scrollBottom = screenRows - 1,
                isCursorVisible = state.isCursorVisible,
                isApplicationCursorKeys = state.isApplicationCursorKeys,
                isAutoWrapEnabled = state.isAutoWrapEnabled
            )
        } else if (!enabled && isAlternateScreen) {
            primaryScreen = savedPrimaryScreen ?: primaryScreen
            state = clampState(
                savedPrimaryState ?: TerminalState(scrollBottom = screenRows - 1),
                columns,
                screenRows
            )
            alternateScreen = null
            savedPrimaryScreen = null
            savedPrimaryState = null
            isAlternateScreen = false
        }
    }

    fun applyGraphicRendition(params: List<Int?>) {
        if (params.isEmpty()) {
            state = state.copy(currentAttribute = TerminalTextAttribute.DEFAULT)
            return
        }

        var attribute = state.currentAttribute
        var index = 0
        while (index < params.size) {
            val code = params[index] ?: 0
            when (code) {
                0 -> attribute = TerminalTextAttribute.DEFAULT
                1 -> attribute = attribute.copy(bold = true)
                2 -> attribute = attribute.copy(dim = true)
                3 -> attribute = attribute.copy(italic = true)
                4 -> attribute = attribute.copy(underline = true)
                5 -> attribute = attribute.copy(blink = true)
                7 -> attribute = attribute.copy(reverse = true)
                8 -> attribute = attribute.copy(hidden = true)
                9 -> attribute = attribute.copy(strikethrough = true)
                22 -> attribute = attribute.copy(bold = false, dim = false)
                23 -> attribute = attribute.copy(italic = false)
                24 -> attribute = attribute.copy(underline = false)
                25 -> attribute = attribute.copy(blink = false)
                27 -> attribute = attribute.copy(reverse = false)
                28 -> attribute = attribute.copy(hidden = false)
                29 -> attribute = attribute.copy(strikethrough = false)
                in 30..37 -> attribute = attribute.copy(foregroundColor = TerminalColor.fromAnsiCode(code))
                39 -> attribute = attribute.copy(foregroundColor = TerminalColor.Default)
                in 40..47 -> attribute = attribute.copy(backgroundColor = TerminalColor.fromAnsiCode(code, isBackground = true))
                49 -> attribute = attribute.copy(backgroundColor = TerminalColor.Default)
                in 90..97 -> attribute = attribute.copy(foregroundColor = TerminalColor.fromAnsiCode(code))
                in 100..107 -> attribute = attribute.copy(backgroundColor = TerminalColor.fromAnsiCode(code, isBackground = true))
                38 -> {
                    when (params.getOrNull(index + 1)) {
                        5 -> {
                            params.getOrNull(index + 2)?.let { paletteIndex ->
                                attribute = attribute.copy(
                                    foregroundColor = TerminalColor.fromPaletteIndex(paletteIndex)
                                )
                            }
                            index += 2
                        }

                        2 -> index += 4
                    }
                }

                48 -> {
                    when (params.getOrNull(index + 1)) {
                        5 -> {
                            params.getOrNull(index + 2)?.let { paletteIndex ->
                                attribute = attribute.copy(
                                    backgroundColor = TerminalColor.fromPaletteIndex(paletteIndex)
                                )
                            }
                            index += 2
                        }

                        2 -> index += 4
                    }
                }
            }
            index++
        }

        state = state.copy(currentAttribute = attribute)
    }

    fun snapshot(): TerminalRendererState {
        val activeScreen = currentScreen()
        val renderRows = if (isAlternateScreen) {
            activeScreen.map { TerminalRenderRow(it.toList()) }
        } else {
            (scrollback + activeScreen.map { it.toList() }).map { TerminalRenderRow(it) }
        }

        val cursorRow = if (isAlternateScreen) {
            state.cursorRow
        } else {
            scrollback.size + state.cursorRow
        }

        return TerminalRendererState(
            renderRows = renderRows,
            columns = columns,
            screenRows = screenRows,
            cursorRow = cursorRow,
            cursorCol = resolvedCursorCol(),
            isCursorVisible = state.isCursorVisible,
            isAlternateScreen = isAlternateScreen,
            scrollbackSize = scrollback.size,
            isApplicationCursorKeys = state.isApplicationCursorKeys,
            isAutoWrapEnabled = state.isAutoWrapEnabled
        )
    }

    private fun putChar(char: Char) {
        if (state.cursorCol >= columns) {
            if (state.isAutoWrapEnabled) {
                carriageReturn()
                lineFeed()
            } else {
                state = state.copy(cursorCol = columns - 1)
            }
        }

        val cursorCol = resolvedCursorCol()
        val screen = currentScreen()
        screen[state.cursorRow][cursorCol] = TerminalCell(
            char = char,
            attribute = state.currentAttribute
        )

        state = if (cursorCol == columns - 1) {
            if (state.isAutoWrapEnabled) {
                state.copy(cursorCol = columns)
            } else {
                state.copy(cursorCol = columns - 1)
            }
        } else {
            state.copy(cursorCol = cursorCol + 1)
        }
    }

    private fun currentScreen(): MutableList<MutableList<TerminalCell>> =
        if (isAlternateScreen) {
            alternateScreen ?: primaryScreen
        } else {
            primaryScreen
        }

    private fun scrollRegionUp(count: Int) {
        repeat(count.coerceAtLeast(1)) {
            val screen = currentScreen()
            val removed = screen.removeAt(state.scrollTop)
            screen.add(state.scrollBottom, blankRow(columns))

            if (!isAlternateScreen && state.scrollTop == 0 && state.scrollBottom == screenRows - 1) {
                scrollback += removed.toList()
                trimScrollback()
            }
        }
    }

    private fun scrollRegionDown(count: Int) {
        repeat(count.coerceAtLeast(1)) {
            val screen = currentScreen()
            screen.removeAt(state.scrollBottom)
            screen.add(state.scrollTop, blankRow(columns))
        }
    }

    private fun trimScrollback() {
        val extra = scrollback.size - scrollbackLimit
        if (extra > 0) {
            repeat(extra) {
                scrollback.removeAt(0)
            }
        }
    }

    private fun clearRow(row: Int) {
        currentScreen()[row] = blankRow(columns)
    }

    private fun fillRange(
        row: MutableList<TerminalCell>,
        startColumn: Int,
        endColumn: Int
    ) {
        if (row.isEmpty()) {
            return
        }
        val clampedStart = startColumn.coerceIn(0, columns - 1)
        val clampedEnd = endColumn.coerceIn(clampedStart, columns - 1)
        for (column in clampedStart..clampedEnd) {
            row[column] = TerminalCell.blank()
        }
    }

    private fun resizeScreen(
        screen: MutableList<MutableList<TerminalCell>>,
        newColumns: Int,
        newRows: Int
    ): MutableList<MutableList<TerminalCell>> {
        val resized = blankScreen(newColumns, newRows)
        val rowCount = minOf(screen.size, newRows)
        val columnCount = minOf(columns, newColumns)
        for (row in 0 until rowCount) {
            for (column in 0 until columnCount) {
                resized[row][column] = screen[row][column]
            }
        }
        return resized
    }

    private fun cloneScreen(
        screen: MutableList<MutableList<TerminalCell>>
    ): MutableList<MutableList<TerminalCell>> =
        screen.map { it.toMutableList() }.toMutableList()

    private fun clampState(
        state: TerminalState,
        columns: Int,
        rows: Int
    ): TerminalState = state.copy(
        cursorRow = state.cursorRow.coerceIn(0, rows - 1),
        cursorCol = state.cursorCol.coerceIn(0, columns),
        savedCursorRow = state.savedCursorRow.coerceIn(0, rows - 1),
        savedCursorCol = state.savedCursorCol.coerceIn(0, columns - 1),
        scrollTop = 0,
        scrollBottom = rows - 1
    )

    private fun resolvedCursorCol(): Int = state.cursorCol.coerceIn(0, columns - 1)

    private fun blankScreen(
        columns: Int,
        rows: Int
    ): MutableList<MutableList<TerminalCell>> =
        MutableList(rows) { blankRow(columns) }

    private fun blankRow(columns: Int): MutableList<TerminalCell> =
        MutableList(columns) { TerminalCell.blank() }
}
