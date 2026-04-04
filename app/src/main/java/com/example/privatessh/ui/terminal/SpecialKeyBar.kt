package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.privatessh.terminal.input.ModifierKey
import com.example.privatessh.terminal.input.ModifierState
import com.example.privatessh.terminal.input.SpecialKey

/**
 * Sticky modifier key bar with special keys for terminal input.
 *
 * Layout:
 * - Top row: Modifier keys (Ctrl, Alt, Shift) with sticky behavior
 * - Bottom row: Special keys (Esc, Tab, Arrows, F-keys, etc.)
 */
@Composable
fun SpecialKeyBar(
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    onModifierClick: (ModifierKey) -> Unit,
    onSpecialKeyClick: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Modifier keys row (sticky behavior)
        ModifierKeyRow(
            activeModifiers = activeModifiers,
            modifierStates = modifierStates,
            onModifierClick = onModifierClick
        )

        // Special keys row
        SpecialKeyRow(
            onSpecialKeyClick = onSpecialKeyClick
        )
    }
}

/**
 * Row of sticky modifier keys (Ctrl, Alt, Shift).
 */
@Composable
private fun ModifierKeyRow(
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    onModifierClick: (ModifierKey) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ModifierKey.entries.forEach { key ->
            val state = modifierStates[key] ?: ModifierState.Inactive
            val isActive = key in activeModifiers
            val isLatched = state == ModifierState.Latched

            ModifierKeyButton(
                key = key,
                isActive = isActive,
                isLatched = isLatched,
                onClick = { onModifierClick(key) }
            )
        }
    }
}

/**
 * Button for a single sticky modifier key.
 * Shows different styles for inactive, one-shot, and latched states.
 */
@Composable
private fun ModifierKeyButton(
    key: ModifierKey,
    isActive: Boolean,
    isLatched: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isLatched -> MaterialTheme.colorScheme.primary
        isActive -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isLatched || isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        modifier = Modifier.size(72.dp, 40.dp)
    ) {
        Text(
            text = when (isLatched) {
                true -> "🔒${key.name.take(1)}"
                false -> key.name
            },
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

/**
 * Scrollable row of special keys.
 */
@Composable
private fun SpecialKeyRow(
    onSpecialKeyClick: (SpecialKey) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        items(SpecialKey.getDisplayKeys()) { key ->
            SpecialKeyButton(
                key = key,
                onClick = { onSpecialKeyClick(key) }
            )
        }
    }
}

/**
 * Button for a single special key.
 */
@Composable
private fun SpecialKeyButton(
    key: SpecialKey,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp, 40.dp)
    ) {
        Text(
            text = key.displayName,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
