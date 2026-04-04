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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.privatessh.R
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
    val reconnectingText = stringResource(R.string.terminal_reconnecting_to, hostName)
    val connectingText = stringResource(R.string.terminal_connecting_to, hostName)
    val disconnectingText = stringResource(R.string.terminal_disconnecting)
    val errorText = stringResource(R.string.terminal_error)
    val disconnectedText = stringResource(R.string.terminal_disconnected)
    val authenticatingText = stringResource(R.string.terminal_authenticating)

    val (statusText, backgroundColor, textColor) = when (lifecycleState) {
        SessionLifecycleState.GRACE ->
            Triple(
                stringResource(R.string.terminal_grace_period, graceMinutesRemaining),
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )

        SessionLifecycleState.RECONNECTING ->
            Triple(
                statusMessage ?: reconnectingText,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )

        SessionLifecycleState.FAILED ->
            Triple(
                statusMessage ?: errorText,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )

        SessionLifecycleState.DISCONNECTING ->
            Triple(
                disconnectingText,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )

        else -> when (sessionState) {
            SshSessionState.CONNECTED ->
                Triple(
                    if (statusMessage != null) {
                        stringResource(R.string.terminal_connected_with_status, hostName, statusMessage)
                    } else {
                        stringResource(R.string.terminal_connected_to, hostName)
                    },
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
            SshSessionState.CONNECTING,
            SshSessionState.RECONNECTING -> Triple(
                connectingText,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            SshSessionState.AUTHENTICATING -> Triple(
                authenticatingText,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            SshSessionState.ERROR -> Triple(
                errorText,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
            SshSessionState.DISCONNECTED -> Triple(
                disconnectedText,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
            SshSessionState.DISCONNECTING -> Triple(
                disconnectingText,
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            color = textColor
        )
    }
}
