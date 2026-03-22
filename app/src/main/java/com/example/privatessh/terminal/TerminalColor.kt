package com.example.privatessh.terminal

/**
 * ANSI color codes for terminal display.
 */
enum class TerminalColor(val code: Int, val isBright: Boolean = false) {
    // Standard colors
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),

    // Bright colors (high intensity)
    BRIGHT_BLACK(0, true),
    BRIGHT_RED(1, true),
    BRIGHT_GREEN(2, true),
    BRIGHT_YELLOW(3, true),
    BRIGHT_BLUE(4, true),
    BRIGHT_MAGENTA(5, true),
    BRIGHT_CYAN(6, true),
    BRIGHT_WHITE(7, true),

    // Special colors
    DEFAULT(9),

    // 256-color palette (simplified)
    COLOR_16(16),
    COLOR_255(255);

    /**
     * Returns the ANSI code for foreground color.
     */
    fun toForegroundCode(): Int {
        return if (isBright) {
            90 + code  // Bright foreground: 90-97
        } else {
            30 + code  // Standard foreground: 30-37
        }
    }

    /**
     * Returns the ANSI code for background color.
     */
    fun toBackgroundCode(): Int {
        return if (isBright) {
            100 + code  // Bright background: 100-107
        } else {
            40 + code  // Standard background: 40-47
        }
    }

    /**
     * Returns true if this is a 256-color palette color.
     */
    fun isPaletteColor(): Boolean {
        return code in 16..255
    }

    companion object {
        /**
         * Parses an ANSI color code to a TerminalColor.
         */
        fun fromAnsiCode(code: Int, isBackground: Boolean): TerminalColor {
            return when {
                code >= 90 && code <= 97 -> {
                    // Bright foreground
                    entries.find { it.code == code - 90 && it.isBright } ?: DEFAULT
                }
                code >= 100 && code <= 107 -> {
                    // Bright background
                    entries.find { it.code == code - 100 && it.isBright } ?: DEFAULT
                }
                code in 30..37 -> {
                    // Standard foreground
                    entries.find { it.code == code - 30 && !it.isBright } ?: DEFAULT
                }
                code in 40..47 -> {
                    // Standard background
                    entries.find { it.code == code - 40 && !it.isBright } ?: DEFAULT
                }
                else -> DEFAULT
            }
        }
    }
}
