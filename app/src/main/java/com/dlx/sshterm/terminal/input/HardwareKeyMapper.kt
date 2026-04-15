package com.dlx.sshterm.terminal.input

import android.view.KeyEvent
import javax.inject.Inject

/**
 * Handles hardware keyboard input with built-in modifier key detection.
 */
class HardwareKeyMapper @Inject constructor(
    private val keyMapper: KeyMapper
) {

    /**
     * Processes a hardware keyboard KeyEvent.
     * @return ByteArray to send to SSH, or null if key should be ignored
     */
    fun handleKeyEvent(
        event: KeyEvent,
        applicationCursorKeys: Boolean = false
    ): ByteArray? {
        // Ignore system keys
        if (keyMapper.isSystemKey(event.keyCode)) {
            return null
        }

        // Only handle ACTION_DOWN events
        if (event.action != KeyEvent.ACTION_DOWN) {
            return null
        }

        // Extract modifiers from hardware keyboard metaState
        val hardwareModifiers = extractModifiers(event)

        // Map the key event with hardware modifiers
        return keyMapper.mapKeyEvent(event, hardwareModifiers, applicationCursorKeys)
    }

    /**
     * Checks if the event has hardware modifier keys pressed.
     */
    fun hasHardwareModifiers(event: KeyEvent): Boolean {
        return event.metaState and (
            KeyEvent.META_CTRL_ON or
            KeyEvent.META_ALT_ON or
            KeyEvent.META_SHIFT_ON
        ) != 0
    }

    /**
     * Extracts modifier keys from a KeyEvent's metaState.
     */
    private fun extractModifiers(event: KeyEvent): Set<ModifierKey> {
        val modifiers = mutableSetOf<ModifierKey>()

        if (event.isCtrlPressed) {
            modifiers.add(ModifierKey.CTRL)
        }
        if (event.isAltPressed) {
            modifiers.add(ModifierKey.ALT)
        }
        if (event.isShiftPressed) {
            modifiers.add(ModifierKey.SHIFT)
        }

        return modifiers
    }
}
