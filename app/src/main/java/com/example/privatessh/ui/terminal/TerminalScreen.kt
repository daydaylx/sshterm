package com.example.privatessh.ui.terminal

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.privatessh.R
import com.example.privatessh.presentation.terminal.TerminalUiEffect
import com.example.privatessh.presentation.terminal.TerminalViewModel
import com.example.privatessh.service.security.BiometricGate
import com.example.privatessh.service.SessionNotificationFactory
import com.example.privatessh.service.SessionLifecycleState
import com.example.privatessh.service.TerminalSessionService
import com.example.privatessh.ui.dialogs.DisconnectDialog
import com.example.privatessh.ui.dialogs.FingerprintDialog

@Composable
fun TerminalScreen(
    hostId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current
    val passwordTitle = stringResource(R.string.terminal_password_required)
    val passwordLabel = stringResource(R.string.terminal_password_label)
    val connectLabel = stringResource(R.string.terminal_connect)
    val cancelLabel = stringResource(R.string.terminal_cancel)
    val terminalInputLabel = stringResource(R.string.terminal_input_label)
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var inputBuffer by remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(hostId) {
        viewModel.connect(hostId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { current ->
            when (current) {
                is TerminalUiEffect.ShowConnectionError -> snackbarHostState.showSnackbar(current.message)
                is TerminalUiEffect.ShowToast -> snackbarHostState.showSnackbar(current.message)
                TerminalUiEffect.NavigateBack -> onNavigateBack()
                TerminalUiEffect.ShowDisconnectDialog -> showDisconnectDialog = true
                TerminalUiEffect.RequestBiometricAuth -> {
                    val activity = context as? AppCompatActivity
                    if (activity != null) {
                        val granted = BiometricGate.authenticate(activity, context.getString(R.string.terminal_biometric_title))
                        viewModel.onBiometricAuthResult(granted)
                    } else {
                        viewModel.onBiometricAuthResult(false)
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(uiState.sessionState, uiState.hostName) {
        when (uiState.sessionState) {
            com.example.privatessh.ssh.SshSessionState.CONNECTED -> {
                context.startTerminalService(sessionId = hostId, hostName = uiState.hostName)
            }
            com.example.privatessh.ssh.SshSessionState.DISCONNECTED -> {
                context.stopService(Intent(context, TerminalSessionService::class.java))
            }
            else -> Unit
        }
    }

    DisposableEffect(uiState.keepScreenOn, uiState.sessionState, uiState.lifecycleState) {
        view.keepScreenOn = uiState.keepScreenOn &&
            (uiState.isConnected || uiState.isReconnecting || uiState.lifecycleState == SessionLifecycleState.GRACE)
        onDispose {
            view.keepScreenOn = false
        }
    }

    if (showDisconnectDialog) {
        DisconnectDialog(
            onConfirm = {
                showDisconnectDialog = false
                viewModel.confirmDisconnect()
            },
            onDismiss = { showDisconnectDialog = false }
        )
    }

    uiState.hostKeyPrompt?.let { prompt ->
        FingerprintDialog(
            hostName = uiState.hostName.ifBlank { context.getString(R.string.terminal_default_host) },
            algorithm = prompt.algorithm,
            fingerprint = prompt.fingerprint,
            onDecision = viewModel::onHostKeyDecision
        )
    }

    if (uiState.isAwaitingPassword) {
        AlertDialog(
            onDismissRequest = viewModel::cancelPasswordPrompt,
            title = { Text(passwordTitle) },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(passwordLabel) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.submitPassword(password)
                            password = ""
                        }
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.submitPassword(password)
                    password = ""
                }) {
                    Text(connectLabel)
                }
            },
            dismissButton = {
                Button(onClick = {
                    password = ""
                    viewModel.cancelPasswordPrompt()
                }) {
                    Text(cancelLabel)
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TerminalTopBar(
                hostName = uiState.hostName,
                sessionState = uiState.sessionState,
                lifecycleState = uiState.lifecycleState,
                canReconnect = uiState.canReconnect,
                hasDiagnostics = uiState.hasDiagnostics,
                latestDiagnosticError = uiState.latestDiagnosticError,
                onDiagnostics = onNavigateToDiagnostics,
                onDisconnect = { showDisconnectDialog = true },
                onReconnect = { viewModel.connect(hostId) }
            )
        },
        bottomBar = {
            Column {
                if (uiState.hasSelection) {
                    TerminalSelectionToolbar(
                        hasSelection = uiState.hasSelection,
                        selectedText = uiState.selectedText,
                        onCopy = {
                            if (uiState.selectedText.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(uiState.selectedText))
                                viewModel.onSelectionCopied()
                            }
                        },
                        onPaste = {
                            val clipboardText = clipboardManager.getText()?.text.orEmpty()
                            if (clipboardText.isNotBlank()) {
                                viewModel.onTextInput(clipboardText)
                                viewModel.clearSelection()
                            }
                        },
                        onCancel = viewModel::clearSelection
                    )
                }
                OutlinedTextField(
                    value = inputBuffer,
                    onValueChange = { newValue ->
                        if (newValue.text.isNotEmpty()) {
                            viewModel.onTextInput(newValue.text)
                        }
                        inputBuffer = TextFieldValue()
                    },
                    label = { Text(terminalInputLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )
                SpecialKeyBar(
                    activeModifiers = uiState.activeModifiers,
                    modifierStates = uiState.modifierStates,
                    onModifierClick = viewModel::onModifierKeyClick,
                    onSpecialKeyClick = viewModel::onSpecialKeyClick
                )
                SessionStatusBar(
                    sessionState = uiState.sessionState,
                    lifecycleState = uiState.lifecycleState,
                    hostName = uiState.hostName,
                    graceMinutesRemaining = uiState.graceMinutesRemaining,
                    statusMessage = uiState.statusMessage
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        TerminalViewport(
            rendererState = uiState.rendererState,
            selection = uiState.selection,
            fontSizeSp = uiState.terminalFontSizeSp,
            onResize = viewModel::onTerminalResize,
            onSelectionStart = viewModel::startSelection,
            onSelectionDrag = viewModel::updateSelection,
            onSelectionClear = viewModel::clearSelection,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

private fun Context.startTerminalService(sessionId: String, hostName: String) {
    val intent = Intent(this, TerminalSessionService::class.java).apply {
        action = SessionNotificationFactory.ACTION_START_SESSION
        putExtra("session_id", sessionId)
        putExtra("host_name", hostName)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}
