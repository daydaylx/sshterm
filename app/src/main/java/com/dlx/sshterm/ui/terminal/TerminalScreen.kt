package com.dlx.sshterm.ui.terminal

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.dlx.sshterm.R
import com.dlx.sshterm.presentation.terminal.TerminalUiEffect
import com.dlx.sshterm.presentation.terminal.TerminalViewModel
import com.dlx.sshterm.service.SessionLifecycleState
import com.dlx.sshterm.service.SessionNotificationFactory
import com.dlx.sshterm.service.TerminalSessionService
import com.dlx.sshterm.service.security.BiometricGate
import com.dlx.sshterm.ssh.SshSessionState
import com.dlx.sshterm.ui.components.AppBackdrop
import com.dlx.sshterm.ui.dialogs.DisconnectDialog
import com.dlx.sshterm.ui.dialogs.FingerprintDialog
import com.dlx.sshterm.ui.theme.AppTheme
import androidx.compose.material3.OutlinedTextField

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
    val imeBridgeController = rememberTerminalImeBridgeController()
    
    var uiVisible by rememberSaveable { mutableStateOf(true) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }

    // Manage Immersive Mode
    DisposableEffect(uiVisible) {
        val window = (context as? AppCompatActivity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowInsetsControllerCompat(window, view)
        
        if (!uiVisible) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

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
                        val granted = BiometricGate.authenticate(
                            activity,
                            context.getString(R.string.terminal_biometric_title)
                        )
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
            SshSessionState.CONNECTED -> {
                context.startTerminalService(sessionId = hostId, hostName = uiState.hostName)
            }
            SshSessionState.DISCONNECTED -> {
                context.stopService(Intent(context, TerminalSessionService::class.java))
            }
            else -> Unit
        }
    }

    LaunchedEffect(uiState.sessionState, uiState.isAwaitingPassword) {
        if (uiState.sessionState == SshSessionState.CONNECTED && !uiState.isAwaitingPassword) {
            imeBridgeController.requestFocus()
        }
    }

    DisposableEffect(uiState.keepScreenOn, uiState.sessionState, uiState.lifecycleState) {
        view.keepScreenOn = uiState.keepScreenOn &&
            (uiState.isConnected || uiState.isReconnecting || uiState.lifecycleState == SessionLifecycleState.GRACE)
        onDispose {
            view.keepScreenOn = false
        }
    }

    BackHandler {
        if (!uiVisible) {
            uiVisible = true
        } else {
            viewModel.navigateBack()
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
            title = { Text(stringResource(R.string.terminal_password_required)) },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.terminal_password_label)) },
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
                    Text(stringResource(R.string.terminal_connect))
                }
            },
            dismissButton = {
                Button(onClick = {
                    password = ""
                    viewModel.cancelPasswordPrompt()
                }) {
                    Text(stringResource(R.string.terminal_cancel))
                }
            }
        )
    }

    AppBackdrop(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { uiVisible = !uiVisible }
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TerminalTopBar(
                        hostName = uiState.hostName,
                        sessionState = uiState.sessionState,
                        lifecycleState = uiState.lifecycleState,
                        canReconnect = uiState.canReconnect,
                        hasDiagnostics = uiState.hasDiagnostics,
                        latestDiagnosticError = uiState.latestDiagnosticError,
                        onDiagnostics = onNavigateToDiagnostics,
                        onDisconnect = { showDisconnectDialog = true },
                        onReconnect = { viewModel.connect(hostId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = if (uiVisible) 8.dp else 0.dp),
                    color = Color.Black,
                    shape = if (uiVisible) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp),
                    border = if (uiVisible) BorderStroke(1.dp, AppTheme.panelBorder.copy(alpha = 0.5f)) else null
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TerminalViewport(
                            rendererState = uiState.rendererState,
                            selection = uiState.selection,
                            fontSizeSp = uiState.terminalFontSizeSp,
                            onResize = viewModel::onTerminalResize,
                            onTapTerminal = {
                                if (!uiVisible) uiVisible = true
                                imeBridgeController.requestFocus()
                            },
                            onSelectionStart = viewModel::startSelection,
                            onSelectionDrag = viewModel::updateSelection,
                            onSelectionClear = viewModel::clearSelection,
                            modifier = Modifier.fillMaxSize()
                        )
                        TerminalImeBridge(
                            controller = imeBridgeController,
                            onTextInput = viewModel::onTextInput,
                            onKeyEvent = viewModel::onKeyEvent,
                            onSpecialKey = viewModel::onSpecialKeyClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(1.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiVisible,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    TerminalAccessoryBar(
                        hasSelection = uiState.hasSelection,
                        activeModifiers = uiState.activeModifiers,
                        modifierStates = uiState.modifierStates,
                        selectedTextLength = uiState.selectedText.length,
                        onCopy = {
                            if (uiState.selectedText.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(uiState.selectedText))
                                viewModel.onSelectionCopied()
                            }
                            imeBridgeController.requestFocus()
                        },
                        onPaste = {
                            val clipboardText = clipboardManager.getText()?.text.orEmpty()
                            if (clipboardText.isNotEmpty()) {
                                viewModel.onTextInput(clipboardText)
                                viewModel.clearSelection()
                            }
                            imeBridgeController.requestFocus()
                        },
                        onCancel = {
                            viewModel.clearSelection()
                            imeBridgeController.requestFocus()
                        },
                        onModifierClick = {
                            viewModel.onModifierKeyClick(it)
                            imeBridgeController.requestFocus()
                        },
                        onSpecialKeyClick = {
                            viewModel.onSpecialKeyClick(it)
                            imeBridgeController.requestFocus()
                        },
                        onTextInput = {
                            viewModel.onTextInput(it)
                            imeBridgeController.requestFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
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
