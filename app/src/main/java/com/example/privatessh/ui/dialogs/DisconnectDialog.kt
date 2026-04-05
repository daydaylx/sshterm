package com.example.privatessh.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.privatessh.R

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
        title = { Text(stringResource(R.string.dialog_disconnect_title)) },
        text = { Text(stringResource(R.string.dialog_disconnect_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_disconnect_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_disconnect_cancel))
            }
        },
        modifier = modifier
    )
}
