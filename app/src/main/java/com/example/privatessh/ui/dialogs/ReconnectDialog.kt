package com.example.privatessh.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog for confirming reconnection to a lost SSH session.
 */
@Composable
fun ReconnectDialog(
    hostName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reconnect to $hostName?") },
        text = {
            Text(
                "The connection to $hostName was lost. " +
                "Do you want to attempt to reconnect?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Reconnect")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
