package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.privatessh.ssh.SshSessionState
import com.example.privatessh.service.SessionLifecycleState

@Composable
fun TerminalTopBar(
    hostName: String,
    sessionState: SshSessionState,
    lifecycleState: SessionLifecycleState,
    canReconnect: Boolean,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(color = statusColor(sessionState, lifecycleState))
            Text(
                text = hostName.ifBlank { "SSH session" },
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row {
            if (sessionState == SshSessionState.CONNECTED || lifecycleState == SessionLifecycleState.GRACE) {
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect")
                }
            } else if (
                lifecycleState != SessionLifecycleState.RECONNECTING &&
                (canReconnect || sessionState == SshSessionState.ERROR || sessionState == SshSessionState.DISCONNECTED)
            ) {
                IconButton(onClick = onReconnect) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                }
            }
        }
    }
}

@Composable
private fun StatusDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, shape = MaterialTheme.shapes.small)
    )
}

@Composable
private fun statusColor(
    state: SshSessionState,
    lifecycleState: SessionLifecycleState
) = when {
    lifecycleState == SessionLifecycleState.GRACE -> MaterialTheme.colorScheme.secondary
    lifecycleState == SessionLifecycleState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
    lifecycleState == SessionLifecycleState.FAILED -> MaterialTheme.colorScheme.error
    state == SshSessionState.CONNECTED -> MaterialTheme.colorScheme.primary
    state in setOf(SshSessionState.CONNECTING, SshSessionState.RECONNECTING, SshSessionState.AUTHENTICATING) ->
        MaterialTheme.colorScheme.tertiary
    state == SshSessionState.ERROR -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}
