package com.example.privatessh.terminal

/**
 * Incremental parser tokens emitted from raw terminal output.
 */
sealed interface TerminalToken {
    data class Text(val value: String) : TerminalToken

    enum class Control : TerminalToken {
        CARRIAGE_RETURN,
        LINE_FEED,
        BACKSPACE,
        TAB,
        BELL
    }

    data class CsiSequence(
        val privateMarker: Char? = null,
        val params: List<Int?> = emptyList(),
        val finalChar: Char
    ) : TerminalToken

    enum class Escape : TerminalToken {
        SAVE_CURSOR,
        RESTORE_CURSOR,
        RESET,
        INDEX,
        NEXT_LINE,
        REVERSE_INDEX
    }
}
