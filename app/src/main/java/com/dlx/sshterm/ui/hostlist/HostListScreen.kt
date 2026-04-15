package com.dlx.sshterm.ui.hostlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dlx.sshterm.R
import com.dlx.sshterm.presentation.hostlist.HostGroup
import com.dlx.sshterm.presentation.hostlist.HostListUiEffect
import com.dlx.sshterm.presentation.hostlist.HostListViewModel
import com.dlx.sshterm.ui.components.AppScreenScaffold
import com.dlx.sshterm.ui.components.ConfirmDialog
import com.dlx.sshterm.ui.components.EmptyStateView
import com.dlx.sshterm.ui.components.HeroPanel
import com.dlx.sshterm.ui.components.LoadingView
import com.dlx.sshterm.ui.components.MetricChip
import com.dlx.sshterm.ui.components.SectionHeader
import com.dlx.sshterm.ui.components.SectionIntro
import com.dlx.sshterm.ui.theme.AppTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.dlx.sshterm.domain.model.HostProfile

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HostListScreen(
    onNavigateToAddHost: () -> Unit,
    onNavigateToEditHost: (String) -> Unit,
    onNavigateToTerminal: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val effect by viewModel.effect.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteCandidate by remember { mutableStateOf<HostProfile?>(null) }
    val tailscaleHostsCount = uiState.hosts.count { it.isTailscale() }
    val keyHostsCount = uiState.hosts.count { it.authType == com.dlx.sshterm.domain.model.AuthType.PRIVATE_KEY }

    LaunchedEffect(effect) {
        when (val current = effect) {
            HostListUiEffect.NavigateToAddHost -> onNavigateToAddHost()
            is HostListUiEffect.NavigateToEditHost -> onNavigateToEditHost(current.hostId)
            is HostListUiEffect.NavigateToTerminal -> onNavigateToTerminal(current.hostId)
            is HostListUiEffect.ShowDeleteDialog -> {
                deleteCandidate = uiState.hosts.firstOrNull { it.id == current.hostId }
            }
            is HostListUiEffect.ShowToast -> snackbarHostState.showSnackbar(current.message)
            is HostListUiEffect.ShowDeleteError -> snackbarHostState.showSnackbar(
                current.cause ?: "Löschen fehlgeschlagen"
            )
            null -> Unit
        }
        if (effect != null) {
            viewModel.clearEffect()
        }
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        title = stringResource(R.string.host_list_title),
        subtitle = stringResource(R.string.host_list_subtitle),
        actions = {
            FilledTonalIconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_settings)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onAddHostClick,
                text = { Text(stringResource(R.string.host_list_add_host)) },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.host_list_add_host)
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingView(
                    message = stringResource(R.string.host_list_loading),
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.isEmpty -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HostListHero(
                            totalHosts = 0,
                            tailscaleHosts = 0,
                            keyHosts = 0,
                            onAddHost = viewModel::onAddHostClick,
                            onOpenSettings = onNavigateToSettings
                        )
                    }
                    item {
                        EmptyStateView(
                            text = stringResource(R.string.host_list_empty_hint),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HostListHero(
                            totalHosts = uiState.hosts.size,
                            tailscaleHosts = tailscaleHostsCount,
                            keyHosts = keyHostsCount,
                            onAddHost = viewModel::onAddHostClick,
                            onOpenSettings = onNavigateToSettings
                        )
                    }

                    uiState.groupedHosts.forEach { (group, hosts) ->
                        stickyHeader {
                            SectionHeader(
                                text = stringResource(group.labelRes()),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        items(
                            items = hosts,
                            key = { it.id }
                        ) { host ->
                            HostCard(
                                host = host,
                                isDeleting = host.id in uiState.isDeleting,
                                onConnect = { viewModel.onConnectClick(host.id) },
                                onEdit = { viewModel.onEditHostClick(host.id) },
                                onDelete = { viewModel.onDeleteClick(host) }
                            )
                        }
                    }

                    item {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(92.dp))
                    }
                }
            }
        }
    }

    val hostToDelete = deleteCandidate
    if (hostToDelete != null) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_host_title),
            message = stringResource(R.string.dialog_delete_host_message, hostToDelete.name),
            confirmText = stringResource(R.string.dialog_delete_host_confirm),
            dismissText = stringResource(R.string.dialog_delete_host_cancel),
            onConfirm = {
                viewModel.onConfirmDelete(hostToDelete.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

private fun HostGroup.labelRes(): Int = when (this) {
    HostGroup.RECENT -> R.string.host_group_recent
    HostGroup.TAILSCALE -> R.string.host_group_tailscale
    HostGroup.DIRECT -> R.string.host_group_direct
    HostGroup.OTHER -> R.string.host_group_other
}

@Composable
private fun HostListHero(
    totalHosts: Int,
    tailscaleHosts: Int,
    keyHosts: Int,
    onAddHost: () -> Unit,
    onOpenSettings: () -> Unit
) {
    HeroPanel(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SectionIntro(
            eyebrow = stringResource(R.string.host_list_hero_eyebrow),
            title = stringResource(R.string.host_list_hero_title),
            supportingText = stringResource(R.string.host_list_hero_body)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricChip(
                label = stringResource(R.string.host_list_metric_hosts),
                value = totalHosts.toString(),
                modifier = Modifier.weight(1f),
                accent = AppTheme.success
            )
            MetricChip(
                label = stringResource(R.string.host_list_metric_tailnet),
                value = tailscaleHosts.toString(),
                modifier = Modifier.weight(1f),
                accent = AppTheme.info
            )
            MetricChip(
                label = stringResource(R.string.host_list_metric_keys),
                value = keyHosts.toString(),
                modifier = Modifier.weight(1f),
                accent = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.FilledTonalButton(
                onClick = onAddHost,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(stringResource(R.string.host_list_add_host))
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(stringResource(R.string.nav_settings))
            }
        }
    }
}
