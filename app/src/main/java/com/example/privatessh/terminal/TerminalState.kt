package com.example.privatessh.terminal

/**
 * Mutable terminal runtime state excluding the screen cell storage itself.
 */
data class TerminalState(
    val cursorRow: Int = 0,
    val cursorCol: Int = 0,
    val savedCursorRow: Int = 0,
    val savedCursorCol: Int = 0,
    val savedApplicationCursorKeys: Boolean = false,
    val savedAutoWrapEnabled: Boolean = true,
    val scrollTop: Int = 0,
    val scrollBottom: Int = 23,
    val currentAttribute: TerminalTextAttribute = TerminalTextAttribute.DEFAULT,
    val isCursorVisible: Boolean = true,
    val isApplicationCursorKeys: Boolean = false,
    val isAutoWrapEnabled: Boolean = true
)
