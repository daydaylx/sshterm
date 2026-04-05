package com.example.privatessh.ui.hostlist

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
import com.example.privatessh.R
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.presentation.hostlist.HostListUiEffect
import com.example.privatessh.presentation.hostlist.HostListViewModel
import com.example.privatessh.ui.components.AppScreenScaffold
import com.example.privatessh.ui.components.ConfirmDialog
import com.example.privatessh.ui.components.EmptyStateView
import com.example.privatessh.ui.components.HeroPanel
import com.example.privatessh.ui.components.LoadingView
import com.example.privatessh.ui.components.MetricChip
import com.example.privatessh.ui.components.SectionIntro
import com.example.privatessh.ui.theme.AppTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

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
    val keyHostsCount = uiState.hosts.count { it.authType == com.example.privatessh.domain.model.AuthType.PRIVATE_KEY }

    LaunchedEffect(effect) {
        when (val current = effect) {
            HostListUiEffect.NavigateToAddHost -> onNavigateToAddHost()
            is HostListUiEffect.NavigateToEditHost -> onNavigateToEditHost(current.hostId)
            is HostListUiEffect.NavigateToTerminal -> onNavigateToTerminal(current.hostId)
            is HostListUiEffect.ShowDeleteDialog -> {
                deleteCandidate = uiState.hosts.firstOrNull { it.id == current.hostId }
            }
            is HostListUiEffect.ShowToast -> snackbarHostState.showSnackbar(current.message)
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
                val groupedHosts = groupHosts(uiState.hosts)
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

                    groupedHosts.forEach { (groupTitle, hosts) ->
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Transparent)
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = groupTitle.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 92.dp))
                    }
                }
            }
        }
    }

    deleteCandidate?.let { host ->
        ConfirmDialog(
            title = stringResource(R.string.dialog_delete_host_title),
            message = stringResource(R.string.dialog_delete_host_message, host.name),
            confirmText = stringResource(R.string.dialog_delete_host_confirm),
            dismissText = stringResource(R.string.dialog_delete_host_cancel),
            onConfirm = {
                viewModel.onConfirmDelete(host.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun groupHosts(hosts: List<HostProfile>): Map<String, List<HostProfile>> {
    val recentTitle = stringResource(R.string.host_group_recent)
    val tailscaleTitle = stringResource(R.string.host_group_tailscale)
    val directTitle = stringResource(R.string.host_group_direct)
    val otherTitle = stringResource(R.string.host_group_other)

    val threeDaysAgo = System.currentTimeMillis() - (86400000 * 3)
    val recentlyConnected = hosts.filter { it.lastConnectedAt != null && it.lastConnectedAt!! > threeDaysAgo }
        .sortedByDescending { it.lastConnectedAt }
    
    val remaining = hosts.filter { it !in recentlyConnected }
    val tailscale = remaining.filter { it.isTailscale() }
    val direct = remaining.filter { !it.isTailscale() }

    return buildMap {
        if (recentlyConnected.isNotEmpty()) put(recentTitle, recentlyConnected)
        if (tailscale.isNotEmpty()) put(tailscaleTitle, tailscale)
        if (direct.isNotEmpty()) put(directTitle, direct)
        if (isEmpty() && hosts.isNotEmpty()) put(otherTitle, hosts)
    }
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
