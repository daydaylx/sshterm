package com.example.privatessh.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        title = { Text("Tailscale Connection Issue") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Could not connect to $hostName via Tailscale."
                )
                Text(
                    "\nCommon solutions:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "\n• Make sure Tailscale is running on your device"
                )
                Text(
                    "• Check that you're logged into your tailnet"
                )
                Text(
                    "• Verify the hostname is correct (e.g., hostname.ts.net)"
                )
                Text(
                    "• Ensure the target device has Tailscale installed and running"
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        modifier = modifier
    )
}
