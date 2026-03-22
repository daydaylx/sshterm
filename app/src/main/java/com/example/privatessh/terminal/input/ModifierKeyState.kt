package com.example.privatessh.terminal.input

import javax.inject.Inject

/**
 * Manages sticky modifier key state machine.
 *
 * Behavior:
 * - Single tap = OneShot (applies to next key only)
 * - Double tap (within 300ms) = Latched (stays on)
 * - Third tap = Release latch
 */
class ModifierKeyState @Inject constructor() {
    private val doubleTapTimeout = 300L
    private val states = mutableMapOf<ModifierKey, ModifierState>()
    private val lastTapTimes = mutableMapOf<ModifierKey, Long>()

    /**
     * Called when a modifier key is tapped.
     * Returns the new state and the set of all active modifiers.
     */
    fun onTap(key: ModifierKey): ModifierState {
        val now = System.currentTimeMillis()
        val lastTap = lastTapTimes[key] ?: 0
        val currentState = states[key]

        val newState = when {
            // Double tap: Inactive/OneShot -> Latched
            currentState != ModifierState.Latched && (now - lastTap) < doubleTapTimeout -> {
                lastTapTimes.remove(key)
                ModifierState.Latched
            }
            // Currently latched: Release
            currentState == ModifierState.Latched -> {
                lastTapTimes.remove(key)
                ModifierState.Inactive
            }
            // Single tap: Inactive -> OneShot
            else -> {
                lastTapTimes[key] = now
                ModifierState.OneShot
            }
        }

        states[key] = newState
        return newState
    }

    /**
     * Called when any regular key is pressed.
     * Returns the set of active modifiers before consumption.
     * OneShot modifiers are consumed, Latched modifiers remain.
     */
    fun onKeyPress(): Set<ModifierKey> {
        val activeModifiers = getActiveModifiers()

        // Consume OneShot modifiers
        states.entries.forEach { (key, state) ->
            if (state == ModifierState.OneShot) {
                states[key] = ModifierState.Inactive
                lastTapTimes.remove(key)
            }
        }

        return activeModifiers
    }

    /**
     * Returns currently active modifiers (OneShot + Latched).
     */
    fun getActiveModifiers(): Set<ModifierKey> {
        return states.filterValues { it != ModifierState.Inactive }.keys
    }

    /**
     * Force release all modifiers.
     */
    fun releaseAll() {
        states.clear()
        lastTapTimes.clear()
    }

    /**
     * Toggle a modifier from latched to inactive (for UI interaction).
     */
    fun toggleLatched(key: ModifierKey) {
        val currentState = states[key]
        if (currentState == ModifierState.Latched) {
            states[key] = ModifierState.Inactive
            lastTapTimes.remove(key)
        }
    }

    /**
     * Returns the current state of a specific modifier key.
     */
    fun getState(key: ModifierKey): ModifierState {
        return states[key] ?: ModifierState.Inactive
    }
}
