package com.example.privatessh.terminal

/**
 * Terminal color model backed by the xterm 256-color palette.
 */
sealed class TerminalColor {
    data object Default : TerminalColor()

    data class Indexed(val value: Int) : TerminalColor() {
        init {
            require(value in 0..255) { "Palette index must be within 0..255." }
        }
    }

    fun isDefault(): Boolean = this is Default

    companion object {
        fun fromAnsiCode(code: Int, isBackground: Boolean = false): TerminalColor {
            val paletteIndex = when {
                !isBackground && code in 30..37 -> code - 30
                isBackground && code in 40..47 -> code - 40
                !isBackground && code in 90..97 -> code - 90 + 8
                isBackground && code in 100..107 -> code - 100 + 8
                else -> return Default
            }

            return Indexed(paletteIndex)
        }

        fun fromPaletteIndex(index: Int): TerminalColor = Indexed(index.coerceIn(0, 255))
    }
}
