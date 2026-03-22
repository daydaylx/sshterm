package com.example.privatessh.ui.hostlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.example.privatessh.ui.components.AppTopBar
import com.example.privatessh.ui.components.ConfirmDialog
import com.example.privatessh.ui.components.EmptyStateView
import com.example.privatessh.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.host_list_title),
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddHostClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.host_list_add_host))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingView(
                    message = "Loading hosts…",
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.isEmpty -> {
                EmptyStateView(
                    text = stringResource(R.string.host_list_empty_hint),
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = uiState.hosts,
                        key = { it.id }
                    ) { host ->
                        HostCard(
                            host = host,
                            isDeleting = host.id in uiState.isDeleting,
                            onClick = { viewModel.onConnectClick(host.id) },
                            onEditClick = { viewModel.onEditHostClick(host.id) },
                            onDeleteClick = { viewModel.onDeleteClick(host) }
                        )
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
