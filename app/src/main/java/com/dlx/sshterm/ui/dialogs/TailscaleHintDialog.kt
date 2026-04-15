package com.dlx.sshterm.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dlx.sshterm.R

/**
 * Dialog showing Tailscale-specific hints and suggestions.
 */
@Composable
fun TailscaleHintDialog(
    hostName: String,
    errorType: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_tailscale_connection_issue)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.dialog_tailscale_connection_error, hostName))
                Text(
                    "\n" + stringResource(R.string.dialog_tailscale_common_solutions),
                    style = MaterialTheme.typography.labelMedium
                )
                Text("\n• " + stringResource(R.string.dialog_tailscale_hint_running))
                Text("• " + stringResource(R.string.dialog_tailscale_hint_logged_in))
                Text("• " + stringResource(R.string.dialog_tailscale_hint_hostname))
                Text("• " + stringResource(R.string.dialog_tailscale_hint_target_running))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        modifier = modifier
    )
}
