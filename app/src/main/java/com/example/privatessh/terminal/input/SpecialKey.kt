package com.example.privatessh.terminal.input

/**
 * Special terminal keys exposed to the terminal UI.
 */
enum class SpecialKey(val displayName: String) {
    // Modifier keys (handled separately by state machine)
    CTRL("Ctrl"),
    ALT("Alt"),
    SHIFT("Shift"),

    // Special characters
    ESC("Esc"),
    TAB("Tab"),
    ENTER("Enter"),

    // Arrow keys
    ARROW_UP("↑"),
    ARROW_DOWN("↓"),
    ARROW_LEFT("←"),
    ARROW_RIGHT("→"),

    // Editing keys
    HOME("Home"),
    END("End"),
    PGUP("PgUp"),
    PGDN("PgDn"),
    INSERT("Ins"),
    DELETE("Del"),

    // Function keys F1-F12
    F1("F1"),
    F2("F2"),
    F3("F3"),
    F4("F4"),
    F5("F5"),
    F6("F6"),
    F7("F7"),
    F8("F8"),
    F9("F9"),
    F10("F10"),
    F11("F11"),
    F12("F12"),

    // Back tab (Shift+Tab)
    BACKTAB("BkTab");

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
