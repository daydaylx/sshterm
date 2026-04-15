package com.dlx.sshterm.terminal

/**
 * Interprets parsed terminal tokens against a mutable terminal buffer.
 */
class AnsiSequenceInterpreter {

    fun apply(tokens: List<TerminalToken>, buffer: TerminalBuffer) {
        tokens.forEach { token -> apply(token, buffer) }
    }

    fun apply(token: TerminalToken, buffer: TerminalBuffer) {
        when (token) {
            is TerminalToken.Text -> buffer.writeText(token.value)
            is TerminalToken.Control -> applyControl(token, buffer)
            is TerminalToken.CsiSequence -> applyCsi(token, buffer)
            is TerminalToken.Escape -> applyEscape(token, buffer)
        }
    }

    private fun applyControl(token: TerminalToken.Control, buffer: TerminalBuffer) {
        when (token) {
            TerminalToken.Control.CARRIAGE_RETURN -> buffer.carriageReturn()
            TerminalToken.Control.LINE_FEED -> buffer.lineFeed()
            TerminalToken.Control.BACKSPACE -> buffer.backspace()
            TerminalToken.Control.TAB -> buffer.tab()
            TerminalToken.Control.BELL -> Unit
        }
    }

    private fun applyEscape(token: TerminalToken.Escape, buffer: TerminalBuffer) {
        when (token) {
            TerminalToken.Escape.SAVE_CURSOR -> buffer.saveCursor()
            TerminalToken.Escape.RESTORE_CURSOR -> buffer.restoreCursor()
            TerminalToken.Escape.RESET -> buffer.reset()
            TerminalToken.Escape.INDEX -> buffer.lineFeed()
            TerminalToken.Escape.NEXT_LINE -> {
                buffer.carriageReturn()
                buffer.lineFeed()
            }
            TerminalToken.Escape.REVERSE_INDEX -> buffer.reverseIndex()
        }
    }

    private fun applyCsi(token: TerminalToken.CsiSequence, buffer: TerminalBuffer) {
        val params = token.params
        when (token.finalChar) {
            '@' -> buffer.insertChars(params.firstOrDefault(1))
            'A' -> buffer.moveCursorRelative(rows = -(params.firstOrDefault(1)), columns = 0)
            'B' -> buffer.moveCursorRelative(rows = params.firstOrDefault(1), columns = 0)
            'C' -> buffer.moveCursorRelative(rows = 0, columns = params.firstOrDefault(1))
            'D' -> buffer.moveCursorRelative(rows = 0, columns = -(params.firstOrDefault(1)))
            'E' -> buffer.cursorNextLine(params.firstOrDefault(1))
            'F' -> buffer.cursorPreviousLine(params.firstOrDefault(1))
            'G' -> buffer.moveCursorColumn(params.firstOrDefault(1) - 1)
            'H', 'f' -> buffer.moveCursorTo(
                row = params.getOrNull(0).defaultTo(1) - 1,
                col = params.getOrNull(1).defaultTo(1) - 1
            )
            'd' -> buffer.moveCursorRow(params.firstOrDefault(1) - 1)
            'J' -> buffer.eraseInDisplay(params.firstOrDefault(0))
            'K' -> buffer.eraseInLine(params.firstOrDefault(0))
            'L' -> buffer.insertLines(params.firstOrDefault(1))
            'M' -> buffer.deleteLines(params.firstOrDefault(1))
            'P' -> buffer.deleteChars(params.firstOrDefault(1))
            'S' -> buffer.scrollUp(params.firstOrDefault(1))
            'T' -> buffer.scrollDown(params.firstOrDefault(1))
            'X' -> buffer.eraseChars(params.firstOrDefault(1))
            'm' -> buffer.applyGraphicRendition(params)
            'r' -> buffer.setScrollRegion(
                top = params.getOrNull(0).defaultTo(1) - 1,
                bottom = params.getOrNull(1).defaultTo(buffer.screenRows) - 1
            )
            's' -> buffer.saveCursor()
            'u' -> buffer.restoreCursor()
            'h' -> applyPrivateMode(token.privateMarker, params, enabled = true, buffer = buffer)
            'l' -> applyPrivateMode(token.privateMarker, params, enabled = false, buffer = buffer)
            else -> Unit
        }
    }

    private fun applyPrivateMode(
        privateMarker: Char?,
        params: List<Int?>,
        enabled: Boolean,
        buffer: TerminalBuffer
    ) {
        if (privateMarker != '?') {
            return
        }

        params.mapNotNull { it }.forEach { mode ->
            when (mode) {
                1 -> buffer.setApplicationCursorKeys(enabled)
                7 -> buffer.setAutoWrapEnabled(enabled)
                25 -> buffer.setCursorVisible(enabled)
                1049 -> buffer.useAlternateScreen(enabled)
            }
        }
    }

    private fun List<Int?>.firstOrDefault(defaultValue: Int): Int =
        firstOrNull().defaultTo(defaultValue)

    private fun Int?.defaultTo(defaultValue: Int): Int =
        this ?: defaultValue
}
