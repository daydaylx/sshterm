package com.example.privatessh.terminal.input

import android.view.KeyEvent
import javax.inject.Inject

/**
 * Maps Android KeyEvents to SSH terminal escape sequences.
 */
class KeyMapper @Inject constructor() {

    /**
     * Maps a KeyEvent to terminal escape sequence.
     * @param event The Android KeyEvent
     * @param activeModifiers Currently active modifier keys
     * @return ByteArray to send to SSH session, or null if key should be ignored
     */
    fun mapKeyEvent(
        event: KeyEvent,
        activeModifiers: Set<ModifierKey>
    ): ByteArray? {
        val keyCode = event.keyCode
        val unicodeChar = event.unicodeChar.toChar()

        // Check if it's a special key first
        val specialKey = getSpecialKey(event.keyCode)
        if (specialKey != null && !specialKey.isModifier()) {
            return specialKey.escapeSequence
        }

        // Handle character input with modifiers
        if (unicodeChar.code > 0) {
            return mapCharWithModifiers(unicodeChar, activeModifiers)
        }

        return null
    }

    /**
     * Maps a character with modifiers to terminal sequence.
     * Handles Ctrl+char, Alt+char combinations.
     */
    fun mapCharWithModifiers(
        char: Char,
        modifiers: Set<ModifierKey>
    ): ByteArray {
        val hasCtrl = ModifierKey.CTRL in modifiers
        val hasAlt = ModifierKey.ALT in modifiers

        return when {
            hasCtrl && hasAlt -> {
                // Ctrl+Alt+char: ESC + Ctrl+char
                byteArrayOf(0x1B.toByte(), mapCtrlChar(char))
            }
            hasAlt -> {
                // Alt+char: ESC + char
                mapAltChar(char)
            }
            hasCtrl -> {
                // Ctrl+char: Control character
                byteArrayOf(mapCtrlChar(char))
            }
            else -> {
                // Plain character
                char.code.toByte().let { byteArrayOf(it) }
            }
        }
    }

    /**
     * Returns SpecialKey for a KeyEvent if it's a special key.
     */
    fun getSpecialKey(keyCode: Int): SpecialKey? {
        return when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> SpecialKey.ESC
            KeyEvent.KEYCODE_TAB -> SpecialKey.TAB
            KeyEvent.KEYCODE_ENTER -> SpecialKey.ENTER
            KeyEvent.KEYCODE_DPAD_UP -> SpecialKey.ARROW_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> SpecialKey.ARROW_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> SpecialKey.ARROW_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> SpecialKey.ARROW_RIGHT
            KeyEvent.KEYCODE_MOVE_HOME -> SpecialKey.HOME
            KeyEvent.KEYCODE_MOVE_END -> SpecialKey.END
            KeyEvent.KEYCODE_PAGE_UP -> SpecialKey.PGUP
            KeyEvent.KEYCODE_PAGE_DOWN -> SpecialKey.PGDN
            KeyEvent.KEYCODE_INSERT -> SpecialKey.INSERT
            KeyEvent.KEYCODE_FORWARD_DEL -> SpecialKey.DELETE
            KeyEvent.KEYCODE_F1 -> SpecialKey.F1
            KeyEvent.KEYCODE_F2 -> SpecialKey.F2
            KeyEvent.KEYCODE_F3 -> SpecialKey.F3
            KeyEvent.KEYCODE_F4 -> SpecialKey.F4
            KeyEvent.KEYCODE_F5 -> SpecialKey.F5
            KeyEvent.KEYCODE_F6 -> SpecialKey.F6
            KeyEvent.KEYCODE_F7 -> SpecialKey.F7
            KeyEvent.KEYCODE_F8 -> SpecialKey.F8
            KeyEvent.KEYCODE_F9 -> SpecialKey.F9
            KeyEvent.KEYCODE_F10 -> SpecialKey.F10
            KeyEvent.KEYCODE_F11 -> SpecialKey.F11
            KeyEvent.KEYCODE_F12 -> SpecialKey.F12
            else -> null
        }
    }

    /**
     * Maps Ctrl+character to terminal control character.
     * Ctrl+A = 0x01, Ctrl+B = 0x02, ..., Ctrl+Z = 0x1A
     * Ctrl+Space = 0x00 (NUL)
     * Ctrl+[ = 0x1B (ESC)
     * Ctrl+? = 0x7F (DEL)
     */
    private fun mapCtrlChar(char: Char): Byte {
        return when (char) {
            in 'a'..'z' -> (char.code - 'a'.code + 1).toByte()
            in 'A'..'Z' -> (char.code - 'A'.code + 1).toByte()
            ' ' -> 0x00.toByte() // NUL
            '[' -> 0x1B.toByte() // ESC
            '?' -> 0x7F.toByte() // DEL
            '@' -> 0x00.toByte() // NUL
            else -> char.code.toByte()
        }
    }

    /**
     * Maps Alt+character to ESC + character sequence.
     */
    private fun mapAltChar(char: Char): ByteArray {
        return byteArrayOf(0x1B.toByte(), char.code.toByte())
    }

    /**
     * Checks if a KeyCode should be handled by the system (back, volume, etc.).
     */
    fun isSystemKey(keyCode: Int): Boolean {
        return keyCode in setOf(
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_ENDCALL
        )
    }
}
