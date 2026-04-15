package com.dlx.sshterm.ui.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dlx.sshterm.R
import com.dlx.sshterm.terminal.input.ModifierKey
import com.dlx.sshterm.terminal.input.ModifierState
import com.dlx.sshterm.terminal.input.SpecialKey
import com.dlx.sshterm.ui.theme.AppTheme

@Composable
fun TerminalAccessoryBar(
    hasSelection: Boolean,
    activeModifiers: Set<ModifierKey>,
    modifierStates: Map<ModifierKey, ModifierState>,
    selectedTextLength: Int,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    onModifierClick: (ModifierKey) -> Unit,
    onSpecialKeyClick: (SpecialKey) -> Unit,
    onTextInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.panelBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Row 1: Modifier & Actions (Sticky)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModifierChip(
                    label = "CTRL",
                    state = modifierStates[ModifierKey.CTRL] ?: ModifierState.Inactive,
                    onClick = { onModifierClick(ModifierKey.CTRL) }
                )
                ModifierChip(
                    label = "ALT",
                    state = modifierStates[ModifierKey.ALT] ?: ModifierState.Inactive,
                    onClick = { onModifierClick(ModifierKey.ALT) }
                )
                ModifierChip(
                    label = "ESC",
                    state = ModifierState.Inactive,
                    onClick = { onSpecialKeyClick(SpecialKey.ESC) }
                )
                ModifierChip(
                    label = "TAB",
                    state = ModifierState.Inactive,
                    onClick = { onSpecialKeyClick(SpecialKey.TAB) }
                )
                
                Box(modifier = Modifier.weight(1f))

                if (hasSelection) {
                    AccessoryIconButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.terminal_copy),
                        onClick = onCopy
                    )
                    AccessoryIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Cancel",
                        onClick = onCancel
                    )
                } else {
                    AccessoryIconButton(
                        icon = Icons.Default.ContentPaste,
                        contentDescription = stringResource(R.string.terminal_paste),
                        onClick = onPaste
                    )
                }
            }

            // Row 2: Scrollable Special Keys & Symbols
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation / Arrows
                KeyChip("↑") { onSpecialKeyClick(SpecialKey.ARROW_UP) }
                KeyChip("↓") { onSpecialKeyClick(SpecialKey.ARROW_DOWN) }
                KeyChip("←") { onSpecialKeyClick(SpecialKey.ARROW_LEFT) }
                KeyChip("→") { onSpecialKeyClick(SpecialKey.ARROW_RIGHT) }
                
                AccessoryDivider()
                
                // Function Keys
                (1..12).forEach { i ->
                    val key = SpecialKey.valueOf("F$i")
                    KeyChip("F$i") { onSpecialKeyClick(key) }
                }
                
                AccessoryDivider()
                
                // Symbols
                listOf("|", "/", "\\", "-", "_", "=", "+", ":", ";", "\"", "'", "<", ">", "?", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "[", "]", "{", "}", "~", "`").forEach { symbol ->
                    KeyChip(symbol) { onTextInput(symbol) }
                }
                
                AccessoryDivider()
                
                // Editing
                KeyChip("BS") { onSpecialKeyClick(SpecialKey.BACKSPACE) }
                KeyChip("ENT") { onSpecialKeyClick(SpecialKey.ENTER) }
                KeyChip("HOME") { onSpecialKeyClick(SpecialKey.HOME) }
                KeyChip("END") { onSpecialKeyClick(SpecialKey.END) }
                KeyChip("PGUP") { onSpecialKeyClick(SpecialKey.PGUP) }
                KeyChip("PGDN") { onSpecialKeyClick(SpecialKey.PGDN) }
                KeyChip("DEL") { onSpecialKeyClick(SpecialKey.DELETE) }
                KeyChip("INS") { onSpecialKeyClick(SpecialKey.INSERT) }
            }
        }
    }
}

@Composable
private fun AccessoryIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AccessoryDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .size(width = 1.dp, height = 20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun ModifierChip(
    label: String,
    state: ModifierState,
    onClick: () -> Unit
) {
    val containerColor = when (state) {
        ModifierState.Latched -> MaterialTheme.colorScheme.primary
        ModifierState.OneShot -> MaterialTheme.colorScheme.primaryContainer
        ModifierState.Inactive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val contentColor = when (state) {
        ModifierState.Latched -> MaterialTheme.colorScheme.onPrimary
        ModifierState.OneShot -> MaterialTheme.colorScheme.onPrimaryContainer
        ModifierState.Inactive -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 34.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun KeyChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

