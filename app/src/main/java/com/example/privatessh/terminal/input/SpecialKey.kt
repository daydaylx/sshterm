package com.example.privatessh.terminal.input

/**
 * Special terminal keys with their ANSI/VT100 escape sequences.
 */
enum class SpecialKey(
    val displayName: String,
    val escapeSequence: ByteArray
) {
    // Modifier keys (handled separately by state machine)
    CTRL("Ctrl", byteArrayOf()),
    ALT("Alt", byteArrayOf()),
    SHIFT("Shift", byteArrayOf()),

    // Special characters
    ESC("Esc", byteArrayOf(0x1B.toByte())),
    TAB("Tab", byteArrayOf(0x09.toByte())),
    ENTER("Enter", byteArrayOf(0x0D.toByte())),

    // Arrow keys (ANSI/VT100)
    ARROW_UP("↑", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x41.toByte())),
    ARROW_DOWN("↓", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x42.toByte())),
    ARROW_LEFT("←", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x44.toByte())),
    ARROW_RIGHT("→", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x43.toByte())),

    // Editing keys
    HOME("Home", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x48.toByte())),
    END("End", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x46.toByte())),
    PGUP("PgUp", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x35.toByte(), 0x7E.toByte())),
    PGDN("PgDn", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x36.toByte(), 0x7E.toByte())),
    INSERT("Ins", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x32.toByte(), 0x7E.toByte())),
    DELETE("Del", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x33.toByte(), 0x7E.toByte())),

    // Function keys F1-F12
    F1("F1", byteArrayOf(0x1B.toByte(), 0x4F.toByte(), 0x50.toByte())),
    F2("F2", byteArrayOf(0x1B.toByte(), 0x4F.toByte(), 0x51.toByte())),
    F3("F3", byteArrayOf(0x1B.toByte(), 0x4F.toByte(), 0x52.toByte())),
    F4("F4", byteArrayOf(0x1B.toByte(), 0x4F.toByte(), 0x53.toByte())),
    F5("F5", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x31.toByte(), 0x35.toByte(), 0x7E.toByte())),
    F6("F6", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x31.toByte(), 0x37.toByte(), 0x7E.toByte())),
    F7("F7", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x31.toByte(), 0x38.toByte(), 0x7E.toByte())),
    F8("F8", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x31.toByte(), 0x39.toByte(), 0x7E.toByte())),
    F9("F9", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x32.toByte(), 0x30.toByte(), 0x7E.toByte())),
    F10("F10", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x32.toByte(), 0x31.toByte(), 0x7E.toByte())),
    F11("F11", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x32.toByte(), 0x33.toByte(), 0x7E.toByte())),
    F12("F12", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x32.toByte(), 0x34.toByte(), 0x7E.toByte())),

    // Back tab (Shift+Tab)
    BACKTAB("BkTab", byteArrayOf(0x1B.toByte(), 0x5B.toByte(), 0x5A.toByte()));

    /**
     * Returns true if this is a modifier key (handled by state machine).
     */
    fun isModifier(): Boolean = this in setOf(CTRL, ALT, SHIFT)

    companion object {
        /**
         * Returns all non-modifier special keys for display in UI.
         */
        fun getDisplayKeys(): List<SpecialKey> {
            return entries.filter { !it.isModifier() }
        }
    }
}
