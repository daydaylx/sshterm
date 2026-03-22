package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Toolbar for terminal selection and copy/paste operations.
 */
@Composable
fun TerminalSelectionToolbar(
    hasSelection: Boolean,
    selectedText: String,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasSelection) {
                "${selectedText.length} chars selected"
            } else {
                "Long-press the terminal to select text"
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f)
        )

        if (hasSelection) {
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
            }
        }

        IconButton(onClick = onPaste) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
        }
    }
}
