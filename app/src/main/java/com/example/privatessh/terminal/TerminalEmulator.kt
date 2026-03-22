package com.example.privatessh.terminal

/**
 * Facade that combines parser, interpreter, and buffer into one terminal engine.
 */
class TerminalEmulator(
    columns: Int = 80,
    rows: Int = 24,
    scrollbackLimit: Int = 2000
) {
    private val parser = TerminalOutputParser()
    private val interpreter = AnsiSequenceInterpreter()
    private val buffer = TerminalBuffer(initialColumns = columns, initialRows = rows, scrollbackLimit = scrollbackLimit)

    fun feed(bytes: ByteArray): TerminalRendererState {
        return try {
            val tokens = parser.feed(bytes)
            interpreter.apply(tokens, buffer)
            buffer.snapshot()
        } catch (_: Exception) {
            buffer.snapshot()
        }
    }

    fun resize(columns: Int, rows: Int): TerminalRendererState {
        buffer.resize(columns, rows)
        return buffer.snapshot()
    }

    fun reset(columns: Int = buffer.columns, rows: Int = buffer.screenRows): TerminalRendererState {
        parser.reset()
        buffer.reset(columns, rows)
        return buffer.snapshot()
    }

    fun setScrollbackLimit(limit: Int) {
        buffer.setScrollbackLimit(limit)
    }

    fun snapshot(): TerminalRendererState = buffer.snapshot()
}
