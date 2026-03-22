package com.example.privatessh.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.privatessh.ssh.SshSessionState
import com.example.privatessh.service.SessionLifecycleState

/**
 * Status bar at the bottom of the terminal showing connection state.
 */
@Composable
fun SessionStatusBar(
    sessionState: SshSessionState,
    lifecycleState: SessionLifecycleState,
    hostName: String,
    graceMinutesRemaining: Int,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    val (statusText, backgroundColor) = when (lifecycleState) {
        SessionLifecycleState.GRACE ->
            "Session in grace period: $graceMinutesRemaining min remaining" to MaterialTheme.colorScheme.secondaryContainer

        SessionLifecycleState.RECONNECTING ->
            (statusMessage ?: "Reconnecting to $hostName...") to MaterialTheme.colorScheme.tertiaryContainer

        SessionLifecycleState.FAILED ->
            (statusMessage ?: "Connection error") to MaterialTheme.colorScheme.errorContainer

        SessionLifecycleState.DISCONNECTING ->
            "Disconnecting..." to MaterialTheme.colorScheme.surfaceVariant

        else -> when (sessionState) {
            SshSessionState.CONNECTED -> "Connected to $hostName" to MaterialTheme.colorScheme.primaryContainer
            SshSessionState.CONNECTING,
            SshSessionState.RECONNECTING -> "Connecting to $hostName..." to MaterialTheme.colorScheme.tertiaryContainer
            SshSessionState.AUTHENTICATING -> "Authenticating..." to MaterialTheme.colorScheme.tertiaryContainer
            SshSessionState.ERROR -> "Connection error" to MaterialTheme.colorScheme.errorContainer
            SshSessionState.DISCONNECTED -> "Disconnected" to MaterialTheme.colorScheme.surfaceVariant
            SshSessionState.DISCONNECTING -> "Disconnecting..." to MaterialTheme.colorScheme.surfaceVariant
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
