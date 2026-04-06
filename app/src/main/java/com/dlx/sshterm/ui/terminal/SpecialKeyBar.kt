package com.dlx.sshterm.ui.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dlx.sshterm.terminal.input.ModifierKey
import com.dlx.sshterm.terminal.input.ModifierState
import com.dlx.sshterm.terminal.input.SpecialKey

@Composable
fun SpecialKeyBar(
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    onModifierClick: (ModifierKey) -> Unit,
    onSpecialKeyClick: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Linke Seite: Modifier + Special Keys kompakt
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kompakte Modifier-Tasten (2-reihig für Platzeffizienz)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CompactModifierKey(
                        key = ModifierKey.CTRL,
                        isActive = ModifierKey.CTRL in activeModifiers,
                        isLatched = modifierStates[ModifierKey.CTRL] == ModifierState.Latched,
                        onClick = { onModifierClick(ModifierKey.CTRL) }
                    )
                    CompactModifierKey(
                        key = ModifierKey.ALT,
                        isActive = ModifierKey.ALT in activeModifiers,
                        isLatched = modifierStates[ModifierKey.ALT] == ModifierState.Latched,
                        onClick = { onModifierClick(ModifierKey.ALT) }
                    )
                    CompactModifierKey(
                        key = ModifierKey.SHIFT,
                        isActive = ModifierKey.SHIFT in activeModifiers,
                        isLatched = modifierStates[ModifierKey.SHIFT] == ModifierState.Latched,
                        onClick = { onModifierClick(ModifierKey.SHIFT) }
                    )
                }
                // Special-Tasten: nur die wichtigsten
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactSpecialKey("ESC", SpecialKey.ESC, onSpecialKeyClick)
                    CompactSpecialKey("TAB", SpecialKey.TAB, onSpecialKeyClick)
                    CompactSpecialKey("ENT", SpecialKey.ENTER, onSpecialKeyClick, Modifier.weight(1f))
                }
            }

            // D-Pad kompakt (36dp statt 44dp)
            CompactDPad(onSpecialKeyClick)
        }
    }
}

@Composable
private fun CompactDPad(onSpecialKeyClick: (SpecialKey) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactArrowButton(Icons.Default.ArrowUpward) { onSpecialKeyClick(SpecialKey.ARROW_UP) }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CompactArrowButton(Icons.Default.ArrowBack) { onSpecialKeyClick(SpecialKey.ARROW_LEFT) }
                CompactArrowButton(Icons.Default.ArrowDownward) { onSpecialKeyClick(SpecialKey.ARROW_DOWN) }
                CompactArrowButton(Icons.Default.ArrowForward) { onSpecialKeyClick(SpecialKey.ARROW_RIGHT) }
            }
        }
    }
}

@Composable
private fun CompactArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CompactModifierKey(
    key: ModifierKey,
    isActive: Boolean,
    isLatched: Boolean,
    onClick: () -> Unit
) {
    // Kurze Bezeichner für Modifier: CT, AL, SH
    val shortLabel = when (key) {
        ModifierKey.CTRL -> "CT"
        ModifierKey.ALT -> "AL"
        ModifierKey.SHIFT -> "SH"
    }

    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp, 28.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = when {
                isLatched -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = when (isLatched) {
                true -> MaterialTheme.colorScheme.onPrimary
                false -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun CompactSpecialKey(
    label: String,
    key: SpecialKey,
    onSpecialKeyClick: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = { onSpecialKeyClick(key) },
        modifier = modifier.size(46.dp, 28.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
