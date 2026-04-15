package com.dlx.sshterm.ui.terminal

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dlx.sshterm.R
import com.dlx.sshterm.service.SessionLifecycleState
import com.dlx.sshterm.ssh.SshSessionState
import com.dlx.sshterm.ui.components.StatusChip
import com.dlx.sshterm.ui.theme.AppTheme

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
    val title = if (hostName.isBlank()) defaultSessionLabel else hostName
    val statusColor = statusColor(sessionState, lifecycleState)
    val supportingText = latestDiagnosticError ?: statusSupportingText(sessionState, lifecycleState)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AppTheme.panelBorder),
        shadowElevation = if (sessionState == SshSessionState.CONNECTED) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = onDiagnostics) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = stringResource(R.string.terminal_diagnostics),
                        tint = if (latestDiagnosticError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
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
