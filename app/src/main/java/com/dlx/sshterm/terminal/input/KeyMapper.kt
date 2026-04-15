package com.dlx.sshterm.terminal.input

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
     * @param applicationCursorKeys Whether the terminal currently expects SS3 cursor keys
     * @return ByteArray to send to SSH session, or null if key should be ignored
     */
    fun mapKeyEvent(
        event: KeyEvent,
        activeModifiers: Set<ModifierKey>,
        applicationCursorKeys: Boolean = false
    ): ByteArray? {
        val unicodeChar = event.unicodeChar.toChar()

        val specialKey = getSpecialKey(
            keyCode = event.keyCode,
            isShiftPressed = event.isShiftPressed
        )
        if (specialKey != null && !specialKey.isModifier()) {
            return mapSpecialKey(specialKey, applicationCursorKeys)
        }

        if (unicodeChar.code > 0) {
            return mapCharWithModifiers(unicodeChar, activeModifiers)
        }

        return null
    }

    fun mapSpecialKey(
        key: SpecialKey,
        applicationCursorKeys: Boolean = false
    ): ByteArray = when (key) {
        SpecialKey.CTRL,
        SpecialKey.ALT,
        SpecialKey.SHIFT -> byteArrayOf()
        SpecialKey.ESC -> byteArrayOf(0x1B.toByte())
        SpecialKey.TAB -> byteArrayOf(0x09.toByte())
        SpecialKey.BACKSPACE -> byteArrayOf(0x7F.toByte())
        SpecialKey.ENTER -> byteArrayOf(0x0D.toByte())
        SpecialKey.ARROW_UP -> cursorKey('A', applicationCursorKeys)
        SpecialKey.ARROW_DOWN -> cursorKey('B', applicationCursorKeys)
        SpecialKey.ARROW_RIGHT -> cursorKey('C', applicationCursorKeys)
        SpecialKey.ARROW_LEFT -> cursorKey('D', applicationCursorKeys)
        SpecialKey.HOME -> if (applicationCursorKeys) ss3('H') else csi("H")
        SpecialKey.END -> if (applicationCursorKeys) ss3('F') else csi("F")
        SpecialKey.PGUP -> csi("5~")
        SpecialKey.PGDN -> csi("6~")
        SpecialKey.INSERT -> csi("2~")
        SpecialKey.DELETE -> csi("3~")
        SpecialKey.F1 -> ss3('P')
        SpecialKey.F2 -> ss3('Q')
        SpecialKey.F3 -> ss3('R')
        SpecialKey.F4 -> ss3('S')
        SpecialKey.F5 -> csi("15~")
        SpecialKey.F6 -> csi("17~")
        SpecialKey.F7 -> csi("18~")
        SpecialKey.F8 -> csi("19~")
        SpecialKey.F9 -> csi("20~")
        SpecialKey.F10 -> csi("21~")
        SpecialKey.F11 -> csi("23~")
        SpecialKey.F12 -> csi("24~")
        SpecialKey.BACKTAB -> csi("Z")
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
            hasCtrl && hasAlt -> byteArrayOf(0x1B.toByte(), mapCtrlChar(char))
            hasAlt -> mapAltChar(char)
            hasCtrl -> byteArrayOf(mapCtrlChar(char))
            else -> char.code.toByte().let { byteArrayOf(it) }
        }
    }

    /**
     * Returns SpecialKey for a KeyEvent if it's a special key.
     */
    fun getSpecialKey(event: KeyEvent): SpecialKey? {
        return getSpecialKey(
            keyCode = event.keyCode,
            isShiftPressed = event.isShiftPressed
        )
    }

    fun getSpecialKey(
        keyCode: Int,
        isShiftPressed: Boolean = false
    ): SpecialKey? {
        return when (keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> SpecialKey.ESC
            KeyEvent.KEYCODE_TAB ->
                if (isShiftPressed) {
                    SpecialKey.BACKTAB
                } else {
                    SpecialKey.TAB
                }
            KeyEvent.KEYCODE_DEL -> SpecialKey.BACKSPACE
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
            ' ' -> 0x00.toByte()
            '[' -> 0x1B.toByte()
            '?' -> 0x7F.toByte()
            '@' -> 0x00.toByte()
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

    private fun cursorKey(finalChar: Char, applicationCursorKeys: Boolean): ByteArray =
        if (applicationCursorKeys) {
            ss3(finalChar)
        } else {
            csi(finalChar.toString())
        }

    private fun ss3(finalChar: Char): ByteArray =
        byteArrayOf(0x1B.toByte(), 'O'.code.toByte(), finalChar.code.toByte())

    private fun csi(sequence: String): ByteArray =
        byteArrayOf(0x1B.toByte(), '['.code.toByte()) + sequence.encodeToByteArray()
}
