package com.example.privatessh.terminal.input

/**
 * State for a single sticky modifier key.
 */
sealed class ModifierState {
    /**
     * Not active - modifier key is not pressed
     */
    data object Inactive : ModifierState()

    /**
     * Active for next keystroke only (single tap)
     */
    data object OneShot : ModifierState()

    /**
     * Latched (double tap) - stays active until tapped again
     */
    data object Latched : ModifierState()
}
