package com.example.privatessh.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.presentation.settings.SettingsUiState
import com.example.privatessh.presentation.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SettingsContent(
            uiState = uiState,
            onGracePeriodChange = viewModel::setGracePeriod,
            onAutoReconnectToggle = viewModel::toggleAutoReconnect,
            onTmuxAutoAttachToggle = viewModel::toggleTmuxAutoAttach,
            onTmuxSessionNameChange = viewModel::setTmuxSessionName,
            onFontSizeChange = viewModel::setTerminalFontSize,
            onBatteryOptimizationToggle = viewModel::toggleBatteryOptimization,
            onTailscaleDetectionToggle = viewModel::toggleTailscaleDetection,
            onKeepScreenOnToggle = viewModel::toggleKeepScreenOn,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onGracePeriodChange: (Int) -> Unit,
    onAutoReconnectToggle: () -> Unit,
    onTmuxAutoAttachToggle: () -> Unit,
    onTmuxSessionNameChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onBatteryOptimizationToggle: () -> Unit,
    onTailscaleDetectionToggle: () -> Unit,
    onKeepScreenOnToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        SettingsSectionHeader("Session")
        SettingsCard(title = "Connection behavior") {
            SettingsSliderItem(
                title = "Grace period",
                description = "Minutes to keep the session alive after the task is removed",
                value = uiState.gracePeriodMinutes,
                valueRange = 1..60,
                onValueChange = onGracePeriodChange
            )
            SettingsSwitchItem(
                title = "Auto reconnect",
                description = "Retry when the SSH connection drops",
                checked = uiState.autoReconnect,
                onCheckedChange = { onAutoReconnectToggle() }
            )
            SettingsSwitchItem(
                title = "Auto-attach tmux",
                description = "Reconnect into tmux manually managed on the server",
                checked = uiState.tmuxAutoAttach,
                onCheckedChange = { onTmuxAutoAttachToggle() }
            )
            OutlinedTextField(
                value = uiState.tmuxSessionName,
                onValueChange = onTmuxSessionNameChange,
                label = { Text("tmux session name") },
                supportingText = { Text("Blank uses the default session name 'main'") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        SettingsSectionHeader("Terminal")
        SettingsCard(title = "Display") {
            SettingsSliderItem(
                title = "Font size",
                description = "Terminal viewport font size",
                value = uiState.terminalFontSize.toInt(),
                valueRange = 10..24,
                onValueChange = { onFontSizeChange(it.toFloat()) }
            )
            SettingsSwitchItem(
                title = "Keep screen on",
                description = "Prevent the screen from sleeping during active sessions",
                checked = uiState.keepScreenOn,
                onCheckedChange = { onKeepScreenOnToggle() }
            )
        }

        SettingsSectionHeader("Network")
        SettingsCard(title = "Tailscale") {
            SettingsSwitchItem(
                title = "Detect Tailscale hosts",
                description = "Show tailored hints for Tailnet targets",
                checked = uiState.tailscaleHostTypeDetection,
                onCheckedChange = { onTailscaleDetectionToggle() }
            )
        }

        SettingsSectionHeader("System")
        SettingsCard(title = "Background execution") {
            SettingsSwitchItem(
                title = "Battery optimization handled",
                description = "Remember whether the device was configured for stable background execution",
                checked = uiState.batteryOptimizationDisabled,
                onCheckedChange = { onBatteryOptimizationToggle() }
            )
        }
    }
}
