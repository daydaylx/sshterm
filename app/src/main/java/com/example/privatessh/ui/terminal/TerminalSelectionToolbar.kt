package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Toolbar for terminal selection and copy/paste operations.
 */
@Composable
fun TerminalSelectionToolbar(
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val selection = remember { mutableStateListOf<String>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection info
        if (selection.isNotEmpty()) {
            Text(
                text = "${selection.size} lines selected",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                onCopy()
                selection.clear()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            }
        }

        // Paste button
        IconButton(onClick = onPaste) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
        }
    }
}

/**
 * Mode for selection toolbar.
 */
enum class SelectionMode {
    IDLE,
    SELECTING,
    SELECTED
}
