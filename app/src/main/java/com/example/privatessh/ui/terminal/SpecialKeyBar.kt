package com.example.privatessh.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.privatessh.terminal.input.ModifierKey
import com.example.privatessh.terminal.input.ModifierState
import com.example.privatessh.terminal.input.SpecialKey

@Composable
fun SpecialKeyBar(
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    onModifierClick: (ModifierKey) -> Unit,
    onSpecialKeyClick: (SpecialKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ModifierKeyRow(
                    activeModifiers = activeModifiers,
                    modifierStates = modifierStates,
                    onModifierClick = onModifierClick
                )
                SpecialKeyRow(onSpecialKeyClick = onSpecialKeyClick)
            }

            ArrowPad(onSpecialKeyClick = onSpecialKeyClick)
        }
    }
}

@Composable
private fun ArrowPad(
    onSpecialKeyClick: (SpecialKey) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArrowButton(Icons.Default.ArrowUpward) { onSpecialKeyClick(SpecialKey.ARROW_UP) }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ArrowButton(Icons.Default.ArrowBack) { onSpecialKeyClick(SpecialKey.ARROW_LEFT) }
                ArrowButton(Icons.Default.ArrowDownward) { onSpecialKeyClick(SpecialKey.ARROW_DOWN) }
                ArrowButton(Icons.Default.ArrowForward) { onSpecialKeyClick(SpecialKey.ARROW_RIGHT) }
            }
        }
    }
}

@Composable
private fun ArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ModifierKeyRow(
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    onModifierClick: (ModifierKey) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModifierKey.entries.forEach { key ->
            val state = modifierStates[key] ?: ModifierState.Inactive
            val isActive = key in activeModifiers
            val isLatched = state == ModifierState.Latched

            ModifierKeyButton(
                key = key,
                isActive = isActive,
                isLatched = isLatched,
                onClick = { onModifierClick(key) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModifierKeyButton(
    key: ModifierKey,
    isActive: Boolean,
    isLatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isActive || isLatched,
        onClick = onClick,
        label = {
            Text(
                text = when {
                    isLatched -> "${key.name} LOCK"
                    isActive -> "${key.name} NEXT"
                    else -> key.name
                },
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier
    )
}

@Composable
private fun SpecialKeyRow(
    onSpecialKeyClick: (SpecialKey) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = MaterialTheme.shapes.medium
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(SpecialKey.getDisplayKeys()) { key ->
                SpecialKeyButton(
                    key = key,
                    onClick = { onSpecialKeyClick(key) }
                )
            }
        }
    }
}

@Composable
private fun SpecialKeyButton(
    key: SpecialKey,
    onClick: () -> Unit
) {
    if (key.displayName.length <= 3) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp, 42.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = key.displayName,
                style = MaterialTheme.typography.labelSmall
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.size(76.dp, 42.dp),
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = key.displayName,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
