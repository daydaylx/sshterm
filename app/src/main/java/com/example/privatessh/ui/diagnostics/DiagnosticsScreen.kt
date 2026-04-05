package com.example.privatessh.ui.diagnostics

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.R
import com.example.privatessh.diagnostics.DiagnosticCategory
import com.example.privatessh.diagnostics.DiagnosticEvent
import com.example.privatessh.diagnostics.DiagnosticLevel
import com.example.privatessh.presentation.diagnostics.DiagnosticsUiState
import com.example.privatessh.presentation.diagnostics.DiagnosticsViewModel
import com.example.privatessh.service.SessionLifecycleState
import com.example.privatessh.ui.components.AppPanel
import com.example.privatessh.ui.components.AppScreenScaffold
import com.example.privatessh.ui.components.EmptyStateView
import com.example.privatessh.ui.components.HeroPanel
import com.example.privatessh.ui.components.MetricChip
import com.example.privatessh.ui.components.SectionIntro
import com.example.privatessh.ui.components.StatusChip
import com.example.privatessh.ui.theme.AppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.diagnostics_copied)
    val copyEmptyMessage = stringResource(R.string.diagnostics_copy_empty)
    val clearedMessage = stringResource(R.string.diagnostics_cleared)

    AppScreenScaffold(
        title = stringResource(R.string.diagnostics_title),
        subtitle = stringResource(R.string.diagnostics_subtitle),
        onNavigateBack = onNavigateBack,
        modifier = modifier.fillMaxSize(),
        actions = {
            FilledTonalIconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(buildClipboardText(context, uiState.events)))
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (uiState.hasEvents) copiedMessage else copyEmptyMessage
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.diagnostics_copy)
                )
            }
            FilledTonalIconButton(
                onClick = {
                    viewModel.clearVisible()
                    scope.launch {
                        snackbarHostState.showSnackbar(clearedMessage)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = stringResource(R.string.diagnostics_clear)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.hasEvents) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DiagnosticsHero(uiState = uiState)
                }
                items(
                    items = uiState.events,
                    key = { it.id }
                ) { event ->
                    DiagnosticEventCard(
                        event = event,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                item {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 20.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { DiagnosticsHero(uiState = uiState) }
                item {
                    EmptyStateView(
                        text = stringResource(R.string.diagnostics_empty),
                        icon = Icons.Default.BugReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsHero(
    uiState: DiagnosticsUiState
) {
    val context = LocalContext.current
    HeroPanel(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SectionIntro(
            eyebrow = stringResource(R.string.diagnostics_hero_eyebrow),
            title = uiState.hostName.ifBlank { stringResource(R.string.diagnostics_hero_title) },
            supportingText = when {
                uiState.isShowingLastSession -> stringResource(R.string.diagnostics_last_session)
                uiState.sessionId != null -> stringResource(R.string.diagnostics_active_session)
                else -> stringResource(R.string.diagnostics_no_session)
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricChip(
                label = stringResource(R.string.diagnostics_metric_events),
                value = uiState.events.size.toString(),
                modifier = Modifier.weight(1f),
                accent = AppTheme.warning
            )
            MetricChip(
                label = stringResource(R.string.diagnostics_metric_state),
                value = lifecycleLabel(context, uiState.lifecycleState),
                modifier = Modifier.weight(1f),
                accent = AppTheme.info
            )
        }
        uiState.statusMessage?.let { statusMessage ->
            AppPanel(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text(
                    text = stringResource(R.string.diagnostics_last_status, statusMessage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiagnosticEventCard(
    event: DiagnosticEvent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
    val accentColor = when (event.level) {
        DiagnosticLevel.INFO -> AppTheme.info
        DiagnosticLevel.WARN -> AppTheme.warning
        DiagnosticLevel.ERROR -> AppTheme.danger
    }

    AppPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        emphasized = event.level == DiagnosticLevel.ERROR,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
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
                        text = event.level.name,
                        color = accentColor
                    )
                    StatusChip(
                        text = categoryLabel(context, event.category),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTimestamp(event.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded && !event.detail.isNullOrBlank()) {
            SelectionContainer {
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun lifecycleLabel(context: Context, state: SessionLifecycleState): String = when (state) {
    SessionLifecycleState.IDLE -> context.getString(R.string.diag_idle)
    SessionLifecycleState.CONNECTED -> context.getString(R.string.diag_connected)
    SessionLifecycleState.DISCONNECTING -> context.getString(R.string.diag_disconnecting)
    SessionLifecycleState.GRACE -> context.getString(R.string.diag_grace)
    SessionLifecycleState.RECONNECTING -> context.getString(R.string.diag_reconnect)
    SessionLifecycleState.FAILED -> context.getString(R.string.diag_failed)
}

private fun categoryLabel(context: Context, category: DiagnosticCategory): String = when (category) {
    DiagnosticCategory.CONNECTION -> context.getString(R.string.diag_cat_connection)
    DiagnosticCategory.AUTH -> context.getString(R.string.diag_cat_auth)
    DiagnosticCategory.HOST_KEY -> context.getString(R.string.diag_cat_host_key)
    DiagnosticCategory.SHELL -> context.getString(R.string.diag_cat_shell)
    DiagnosticCategory.KEEPALIVE -> context.getString(R.string.diag_cat_keepalive)
    DiagnosticCategory.SERVICE -> context.getString(R.string.diag_cat_service)
    DiagnosticCategory.UI -> context.getString(R.string.diag_cat_ui)
}

private fun formatTimestamp(timestampMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    return formatter.format(
        Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
    )
}

private fun buildClipboardText(context: Context, events: List<DiagnosticEvent>): String {
    if (events.isEmpty()) {
        return ""
    }

    return events.joinToString(separator = "\n\n") { event ->
        buildString {
            append("[${event.level}] ${categoryLabel(context, event.category)}")
            append(" · ${formatTimestamp(event.timestampMillis)}")
            append('\n')
            append(event.title)
            event.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                append('\n')
                append(detail)
            }
        }
    }
}
