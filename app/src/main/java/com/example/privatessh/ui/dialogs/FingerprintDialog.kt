package com.example.privatessh.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.privatessh.R
import com.example.privatessh.ssh.hostkey.HostKeyDecision

/**
 * Dialog for confirming an unknown SSH host key fingerprint.
 */
@Composable
fun FingerprintDialog(
    hostName: String,
    algorithm: String,
    fingerprint: String,
    onDecision: (HostKeyDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { onDecision(HostKeyDecision.Reject) },
        title = { Text(stringResource(R.string.dialog_fingerprint_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.dialog_fingerprint_intro, hostName)
                )
                Text(
                    algorithm,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    fingerprint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    stringResource(R.string.dialog_fingerprint_continue),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDecision(HostKeyDecision.TrustAlways(fingerprint)) }
            ) {
                Text(stringResource(R.string.dialog_fingerprint_trust_always))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDecision(HostKeyDecision.TrustOnce(fingerprint)) }
            ) {
                Text(stringResource(R.string.dialog_fingerprint_trust_once))
            }
        },
        modifier = modifier
    )
}
