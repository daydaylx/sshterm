package com.example.privatessh.terminal

/**
 * ANSI color and style attributes for terminal text.
 */
data class TerminalTextAttribute(
    val foregroundColor: TerminalColor = TerminalColor.Default,
    val backgroundColor: TerminalColor = TerminalColor.Default,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val blink: Boolean = false,
    val reverse: Boolean = false,
    val hidden: Boolean = false,
    val strikethrough: Boolean = false
) {
    companion object {
        val DEFAULT = TerminalTextAttribute()
    }

    /**
     * Returns true if this is the default attribute.
     */
    fun isDefault(): Boolean {
        return this == DEFAULT
    }

    /**
     * Creates a new attribute with the given foreground color.
     */
    fun withForeground(color: TerminalColor): TerminalTextAttribute {
        return copy(foregroundColor = color)
    }

    /**
     * Creates a new attribute with the given background color.
     */
    fun withBackground(color: TerminalColor): TerminalTextAttribute {
        return copy(backgroundColor = color)
    }

    /**
     * Creates a new attribute with bold enabled.
     */
    fun withBold(): TerminalTextAttribute {
        return copy(bold = true)
    }

    /**
     * Creates a new attribute with underline enabled.
     */
    fun withUnderline(): TerminalTextAttribute {
        return copy(underline = true)
    }
}
