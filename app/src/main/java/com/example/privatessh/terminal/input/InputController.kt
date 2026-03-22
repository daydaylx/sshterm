package com.example.privatessh.terminal.input

import android.view.KeyEvent
import com.example.privatessh.ssh.io.InputWriter
import com.example.privatessh.terminal.TerminalRendererState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Central controller for terminal input.
 * Coordinates between UI, key mapping, and SSH layer.
 */
@ViewModelScoped
class InputController @Inject constructor(
    private val inputWriter: InputWriter,
    private val keyMapper: KeyMapper,
    private val hardwareKeyMapper: HardwareKeyMapper,
    private val modifierKeyState: ModifierKeyState
) {

    /**
     * Handles input from software keyboard (IME).
     */
    suspend fun handleSoftwareInput(text: String) {
        withContext(Dispatchers.IO) {
            // Get active modifiers before sending text
            val activeModifiers = modifierKeyState.onKeyPress()

            // Apply modifiers to each character if needed
            if (activeModifiers.isNotEmpty()) {
                text.forEach { char ->
                    val bytes = keyMapper.mapCharWithModifiers(char, activeModifiers)
                    write(bytes)
                }
            } else {
                // No modifiers, send plain text
                inputWriter.writeString(text)
            }
        }
    }

    /**
     * Handles KeyEvent from any source (software or hardware keyboard).
     * Returns true if the key was handled, false if it should be ignored.
     */
    suspend fun handleKeyEvent(
        event: KeyEvent,
        rendererState: TerminalRendererState
    ): Boolean {
        withContext(Dispatchers.IO) {
            // Ignore system keys
            if (keyMapper.isSystemKey(event.keyCode)) {
                return@withContext false
            }

            // Check for hardware keyboard modifiers
            if (hardwareKeyMapper.hasHardwareModifiers(event)) {
                // Hardware keyboard: use hardware modifiers, clear sticky ones
                modifierKeyState.releaseAll()
                val bytes = hardwareKeyMapper.handleKeyEvent(
                    event = event,
                    applicationCursorKeys = rendererState.isApplicationCursorKeys
                )
                if (bytes != null) {
                    write(bytes)
                }
                return@withContext true
            }

            // Software keyboard / virtual keys: check active sticky modifiers
            val activeModifiers = modifierKeyState.getActiveModifiers()
            if (activeModifiers.isNotEmpty()) {
                val bytes = keyMapper.mapKeyEvent(
                    event = event,
                    activeModifiers = activeModifiers,
                    applicationCursorKeys = rendererState.isApplicationCursorKeys
                )
                if (bytes != null) {
                    write(bytes)
                    // Consume one-shot modifiers after sending
                    modifierKeyState.onKeyPress()
                }
            } else {
                // No modifiers, map directly
                val bytes = keyMapper.mapKeyEvent(
                    event = event,
                    activeModifiers = emptySet(),
                    applicationCursorKeys = rendererState.isApplicationCursorKeys
                )
                if (bytes != null) {
                    write(bytes)
                }
            }

            return@withContext true
        }
        return false // Should not reach here
    }

    /**
     * Handles special key button click.
     */
    suspend fun handleSpecialKey(
        key: SpecialKey,
        rendererState: TerminalRendererState
    ) {
        withContext(Dispatchers.IO) {
            // Special keys don't consume modifiers
            write(
                keyMapper.mapSpecialKey(
                    key = key,
                    applicationCursorKeys = rendererState.isApplicationCursorKeys
                )
            )
        }
    }

    /**
     * Handles modifier key tap.
     * Returns the set of currently active modifiers after the tap.
     */
    fun handleModifierTap(key: ModifierKey): Set<ModifierKey> {
        modifierKeyState.onTap(key)
        return modifierKeyState.getActiveModifiers()
    }

    /**
     * Releases all modifier keys.
     */
    fun releaseAllModifiers() {
        modifierKeyState.releaseAll()
    }

    /**
     * Returns the current state of a specific modifier key.
     */
    fun getModifierState(key: ModifierKey): ModifierState {
        return modifierKeyState.getState(key)
    }

    /**
     * Returns all currently active modifiers.
     */
    fun getActiveModifiers(): Set<ModifierKey> {
        return modifierKeyState.getActiveModifiers()
    }

    /**
     * Writes data to SSH session.
     */
    private suspend fun write(data: ByteArray) {
        inputWriter.write(data)
    }
}
