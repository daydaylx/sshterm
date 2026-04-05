package com.example.privatessh.ui.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.privatessh.R
import com.example.privatessh.service.SessionLifecycleState
import com.example.privatessh.ssh.SshSessionState
import com.example.privatessh.ui.components.AppPanel
import com.example.privatessh.ui.components.StatusChip
import com.example.privatessh.ui.theme.AppTheme

@Composable
fun TerminalTopBar(
    hostName: String,
    sessionState: SshSessionState,
    lifecycleState: SessionLifecycleState,
    canReconnect: Boolean,
    hasDiagnostics: Boolean,
    latestDiagnosticError: String?,
    onDiagnostics: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultSessionLabel = stringResource(R.string.terminal_default_session)
    val statusColor = statusColor(sessionState, lifecycleState)

    AppPanel(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        emphasized = sessionState == SshSessionState.CONNECTED
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        text = lifecycleLabel(sessionState, lifecycleState),
                        color = statusColor
                    )
                    if (hasDiagnostics) {
                        StatusChip(
                            text = if (latestDiagnosticError != null) {
                                stringResource(R.string.terminal_topbar_warning)
                            } else {
                                stringResource(R.string.terminal_topbar_trace)
                            },
                            color = if (latestDiagnosticError != null) AppTheme.danger else AppTheme.info
                        )
                    }
                }
                Text(
                    text = if (hostName.isBlank()) defaultSessionLabel else hostName,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = statusSupportingText(sessionState, lifecycleState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = onDiagnostics) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = stringResource(R.string.terminal_diagnostics),
                        tint = if (latestDiagnosticError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    )
                }
                if (sessionState == SshSessionState.CONNECTED || lifecycleState == SessionLifecycleState.GRACE) {
                    FilledTonalIconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.terminal_disconnect)
                        )
                    }
                } else if (
                    lifecycleState != SessionLifecycleState.RECONNECTING &&
                    (sessionState == SshSessionState.ERROR || sessionState == SshSessionState.DISCONNECTED)
                ) {
                    FilledTonalIconButton(
                        onClick = onReconnect,
                        enabled = canReconnect
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.terminal_reconnect)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(
    state: SshSessionState,
    lifecycleState: SessionLifecycleState
): Color = when {
    lifecycleState == SessionLifecycleState.GRACE -> AppTheme.warning
    lifecycleState == SessionLifecycleState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
    lifecycleState == SessionLifecycleState.FAILED -> AppTheme.danger
    state == SshSessionState.CONNECTED -> AppTheme.success
    state in setOf(SshSessionState.CONNECTING, SshSessionState.RECONNECTING, SshSessionState.AUTHENTICATING) ->
        AppTheme.warning
    state == SshSessionState.ERROR -> AppTheme.danger
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun lifecycleLabel(
    state: SshSessionState,
    lifecycleState: SessionLifecycleState
): String = when {
    lifecycleState == SessionLifecycleState.GRACE -> stringResource(R.string.top_bar_grace)
    lifecycleState == SessionLifecycleState.RECONNECTING -> stringResource(R.string.top_bar_reconnect)
    lifecycleState == SessionLifecycleState.FAILED -> stringResource(R.string.top_bar_error)
    state == SshSessionState.CONNECTED -> stringResource(R.string.top_bar_live)
    state == SshSessionState.AUTHENTICATING -> stringResource(R.string.top_bar_auth)
    state == SshSessionState.CONNECTING || state == SshSessionState.RECONNECTING -> stringResource(R.string.top_bar_connecting)
    else -> stringResource(R.string.top_bar_offline)
}

@Composable
private fun statusSupportingText(
    state: SshSessionState,
    lifecycleState: SessionLifecycleState
): String = when {
    lifecycleState == SessionLifecycleState.GRACE ->
        stringResource(R.string.top_bar_msg_grace)
    lifecycleState == SessionLifecycleState.RECONNECTING ->
        stringResource(R.string.top_bar_msg_reconnect)
    lifecycleState == SessionLifecycleState.FAILED || state == SshSessionState.ERROR ->
        stringResource(R.string.top_bar_msg_error)
    state == SshSessionState.CONNECTED ->
        stringResource(R.string.top_bar_msg_live)
    state == SshSessionState.AUTHENTICATING ->
        stringResource(R.string.top_bar_msg_auth)
    else ->
        stringResource(R.string.top_bar_msg_offline)
}
