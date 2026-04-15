package com.dlx.sshterm.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dlx.sshterm.R

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
        title = { Text(stringResource(R.string.dialog_reconnect_title, hostName)) },
        text = { Text(stringResource(R.string.dialog_reconnect_message, hostName)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_reconnect_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_reconnect_cancel))
            }
        },
        modifier = modifier
    )
}
