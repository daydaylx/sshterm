package com.example.privatessh.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.R
import com.example.privatessh.presentation.settings.SettingsUiState
import com.example.privatessh.presentation.settings.SettingsViewModel
import com.example.privatessh.ui.components.AppScreenScaffold
import com.example.privatessh.ui.components.HeroPanel
import com.example.privatessh.ui.components.MetricChip
import com.example.privatessh.ui.components.SectionIntro
import com.example.privatessh.ui.theme.AppTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        onNavigateBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SettingsContent(
            uiState = uiState,
            onGracePeriodChange = viewModel::setGracePeriod,
            onAutoReconnectToggle = viewModel::toggleAutoReconnect,
            onTmuxAutoAttachToggle = viewModel::toggleTmuxAutoAttach,
            onTmuxSessionNameChange = viewModel::setTmuxSessionName,
            onFontSizeChange = viewModel::setTerminalFontSize,
            onScrollbackSizeChange = viewModel::setScrollbackSize,
            onBatteryOptimizationToggle = viewModel::toggleBatteryOptimization,
            onTailscaleDetectionToggle = viewModel::toggleTailscaleDetection,
            onKeepScreenOnToggle = viewModel::toggleKeepScreenOn,
            onBiometricAuthToggle = viewModel::toggleBiometricAuth,
            onDiagnosticsClick = onNavigateToDiagnostics,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
    onScrollbackSizeChange: (Int) -> Unit,
    onBatteryOptimizationToggle: () -> Unit,
    onTailscaleDetectionToggle: () -> Unit,
    onKeepScreenOnToggle: () -> Unit,
    onBiometricAuthToggle: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeroPanel {
            SectionIntro(
                eyebrow = stringResource(R.string.settings_hero_eyebrow),
                title = stringResource(R.string.settings_hero_title),
                supportingText = stringResource(R.string.settings_hero_body)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip(
                    label = stringResource(R.string.settings_grace_period),
                    value = uiState.gracePeriodMinutes.toString(),
                    modifier = Modifier.weight(1f),
                    accent = AppTheme.warning
                )
                MetricChip(
                    label = stringResource(R.string.settings_terminal_font),
                    value = uiState.terminalFontSize.toInt().toString(),
                    modifier = Modifier.weight(1f),
                    accent = AppTheme.info
                )
                MetricChip(
                    label = stringResource(R.string.settings_biometric_auth),
                    value = if (uiState.biometricAuthEnabled) {
                        stringResource(R.string.settings_metric_enabled)
                    } else {
                        stringResource(R.string.settings_metric_disabled)
                    },
                    modifier = Modifier.weight(1f),
                    accent = AppTheme.success
                )
            }
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_session))
        SettingsCard {
            SettingsSliderItem(
                title = stringResource(R.string.settings_grace_period),
                description = stringResource(R.string.settings_grace_period_description),
                value = uiState.gracePeriodMinutes,
                valueRange = 1..60,
                onValueChange = onGracePeriodChange
            )
            SettingsSwitchItem(
                title = stringResource(R.string.settings_auto_reconnect),
                description = stringResource(R.string.settings_auto_reconnect_description),
                checked = uiState.autoReconnect,
                onCheckedChange = { onAutoReconnectToggle() },
                icon = Icons.Default.Refresh
            )
            SettingsSwitchItem(
                title = stringResource(R.string.settings_tmux_attach),
                description = stringResource(R.string.settings_tmux_attach_description),
                checked = uiState.tmuxAutoAttach,
                onCheckedChange = { onTmuxAutoAttachToggle() },
                icon = Icons.Default.Terminal
            )
            OutlinedTextField(
                value = uiState.tmuxSessionName,
                onValueChange = onTmuxSessionNameChange,
                label = { Text(stringResource(R.string.settings_tmux_session_name)) },
                supportingText = { Text(stringResource(R.string.settings_tmux_session_name_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_terminal))
        SettingsCard {
            SettingsSliderItem(
                title = stringResource(R.string.settings_terminal_font),
                description = stringResource(R.string.settings_terminal_font_description),
                value = uiState.terminalFontSize.toInt(),
                valueRange = 10..24,
                onValueChange = { onFontSizeChange(it.toFloat()) }
            )
            SettingsSliderItem(
                title = stringResource(R.string.settings_scrollback_buffer),
                description = stringResource(R.string.settings_scrollback_buffer_description),
                value = uiState.terminalScrollbackSize,
                valueRange = 100..5000,
                onValueChange = onScrollbackSizeChange
            )
            SettingsSwitchItem(
                title = stringResource(R.string.settings_keep_screen_on),
                description = stringResource(R.string.settings_keep_screen_on_description),
                checked = uiState.keepScreenOn,
                onCheckedChange = { onKeepScreenOnToggle() },
                icon = Icons.Default.Monitor
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_network))
        SettingsCard {
            SettingsSwitchItem(
                title = stringResource(R.string.settings_detect_tailscale),
                description = stringResource(R.string.settings_detect_tailscale_description),
                checked = uiState.tailscaleHostTypeDetection,
                onCheckedChange = { onTailscaleDetectionToggle() },
                icon = Icons.Default.Lan
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_security))
        SettingsCard {
            SettingsSwitchItem(
                title = stringResource(R.string.settings_biometric_auth),
                description = stringResource(R.string.settings_biometric_auth_description),
                checked = uiState.biometricAuthEnabled,
                onCheckedChange = { onBiometricAuthToggle() },
                icon = Icons.Default.Fingerprint
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_system))
        SettingsCard {
            SettingsSwitchItem(
                title = stringResource(R.string.settings_battery_optimization_handled),
                description = stringResource(R.string.settings_battery_optimization_handled_description),
                checked = uiState.batteryOptimizationDisabled,
                onCheckedChange = { onBatteryOptimizationToggle() },
                icon = Icons.Default.BatteryFull
            )
        }

        SettingsSectionHeader(stringResource(R.string.settings_section_diagnostics))
        SettingsCard {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_card_diagnostics)) },
                supportingContent = { Text(stringResource(R.string.settings_diagnostics_description)) },
                leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                trailingContent = {
                    FilledTonalButton(onClick = onDiagnosticsClick) {
                        Text(stringResource(R.string.settings_open_diagnostics))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}
