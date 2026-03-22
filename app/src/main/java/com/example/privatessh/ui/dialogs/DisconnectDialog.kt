package com.example.privatessh.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Confirmation dialog for disconnecting from SSH session.
 */
@Composable
fun DisconnectDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disconnect Session?") },
        text = {
            Text(
                "Do you want to disconnect from the SSH session? " +
                "This will close the terminal connection."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Disconnect")
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
