package com.example.privatessh.presentation.terminal

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.usecase.host.GetHostByIdUseCase
import com.example.privatessh.domain.usecase.session.ObserveSessionUseCase
import com.example.privatessh.domain.usecase.session.ResizeTerminalUseCase
import com.example.privatessh.domain.usecase.session.StartSessionUseCase
import com.example.privatessh.domain.usecase.session.StopSessionUseCase
import com.example.privatessh.domain.usecase.settings.GetSettingsUseCase
import com.example.privatessh.ssh.auth.PasswordAuthStrategy
import com.example.privatessh.ssh.auth.PrivateKeyAuthStrategy
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import com.example.privatessh.service.SessionRegistry
import com.example.privatessh.terminal.TerminalCellPosition
import com.example.privatessh.terminal.TerminalSelection
import com.example.privatessh.terminal.extractSelectionText
import com.example.privatessh.terminal.input.InputController
import com.example.privatessh.terminal.input.ModifierKey
import com.example.privatessh.terminal.input.SpecialKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val inputController: InputController,
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val resizeTerminalUseCase: ResizeTerminalUseCase,
    private val getHostByIdUseCase: GetHostByIdUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val sessionRegistry: SessionRegistry,
    private val passwordAuthStrategy: PasswordAuthStrategy,
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<TerminalUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    @Volatile private var currentHost: HostProfile? = null
    @Volatile private var pendingHostKeyDecision: CompletableDeferred<HostKeyDecision>? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { sessionState ->
                _uiState.value = _uiState.value.copy(
                    sessionState = sessionState,
                    selection = if (sessionState == com.example.privatessh.ssh.SshSessionState.CONNECTED) {
                        _uiState.value.selection
                    } else {
                        null
                    },
                    selectedText = if (sessionState == com.example.privatessh.ssh.SshSessionState.CONNECTED) {
                        _uiState.value.selectedText
                    } else {
                        ""
                    }
                )
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeErrors().collect { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeTerminalRendererState().collect { rendererState ->
                _uiState.value = _uiState.value.copy(
                    rendererState = rendererState,
                    terminalColumns = rendererState.columns,
                    terminalRows = rendererState.screenRows
                )
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeCurrentHost().collect { host ->
                _uiState.value = _uiState.value.copy(hostName = host?.getDisplayName().orEmpty())
            }
        }
        viewModelScope.launch {
            sessionRegistry.runtimeState.collect { runtimeState ->
                val sessionHostName = runtimeState.activeSession?.hostName
                _uiState.value = _uiState.value.copy(
                    lifecycleState = runtimeState.lifecycleState,
                    canReconnect = runtimeState.canReconnect,
                    graceMinutesRemaining = runtimeState.graceMinutesRemaining,
                    statusMessage = runtimeState.statusMessage,
                    hostName = sessionHostName ?: _uiState.value.hostName
                )
            }
        }
        viewModelScope.launch {
            getSettingsUseCase.observeKeepScreenOn().collect { keepScreenOn ->
                _uiState.value = _uiState.value.copy(keepScreenOn = keepScreenOn)
            }
        }
        viewModelScope.launch {
            getSettingsUseCase.observeTerminalMetrics().collect { metrics ->
                _uiState.value = _uiState.value.copy(terminalFontSizeSp = metrics.fontSize)
            }
        }
        updateModifierStates()
    }

    fun connect(hostId: String) {
        viewModelScope.launch {
            val activeHost = observeSessionUseCase.getCurrentHost()
            val currentState = observeSessionUseCase.getCurrentState()
            if (
                activeHost?.id == hostId &&
                currentState in setOf(
                    com.example.privatessh.ssh.SshSessionState.CONNECTED,
                    com.example.privatessh.ssh.SshSessionState.CONNECTING,
                    com.example.privatessh.ssh.SshSessionState.RECONNECTING,
                    com.example.privatessh.ssh.SshSessionState.AUTHENTICATING
                )
            ) {
                currentHost = activeHost
                _uiState.value = _uiState.value.copy(hostName = activeHost.getDisplayName(), error = null)
                return@launch
            }

            val host = getHostByIdUseCase(hostId)
            if (host == null) {
                _effect.trySend(TerminalUiEffect.ShowConnectionError("Host not found"))
                return@launch
            }

            currentHost = host
            _uiState.value = _uiState.value.copy(hostName = host.getDisplayName(), error = null)

            when (host.authType) {
                AuthType.PASSWORD -> {
                    if (!passwordAuthStrategy.hasPassword(host.id)) {
                        _uiState.value = _uiState.value.copy(isAwaitingPassword = true)
                        return@launch
                    }
                }
                AuthType.PRIVATE_KEY -> {
                    if (!privateKeyAuthStrategy.hasPrivateKey(host.id)) {
                        _effect.trySend(TerminalUiEffect.ShowConnectionError("No private key stored for this host"))
                        return@launch
                    }
                }
            }

            connectCurrentHost()
        }
    }

    fun submitPassword(password: String) {
        val host = currentHost ?: return
        passwordAuthStrategy.setPassword(host.id, password)
        _uiState.value = _uiState.value.copy(isAwaitingPassword = false)
        connectCurrentHost()
    }

    fun cancelPasswordPrompt() {
        _uiState.value = _uiState.value.copy(isAwaitingPassword = false)
        _effect.trySend(TerminalUiEffect.NavigateBack)
    }

    fun onHostKeyDecision(decision: HostKeyDecision) {
        pendingHostKeyDecision?.complete(decision)
        pendingHostKeyDecision = null
        _uiState.value = _uiState.value.copy(hostKeyPrompt = null)
    }

    fun disconnect() {
        clearSelection()
        viewModelScope.launch {
            stopSessionUseCase()
        }
    }

    fun onTextInput(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            inputController.handleSoftwareInput(text)
            updateModifierStates()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        viewModelScope.launch {
            inputController.handleKeyEvent(event, _uiState.value.rendererState)
            updateModifierStates()
        }
        return true
    }

    fun onSpecialKeyClick(key: SpecialKey) {
        viewModelScope.launch {
            inputController.handleSpecialKey(key, _uiState.value.rendererState)
            _effect.trySend(TerminalUiEffect.Vibrate)
        }
    }

    fun onModifierKeyClick(key: ModifierKey) {
        inputController.handleModifierTap(key)
        updateModifierStates()
        _effect.trySend(TerminalUiEffect.Vibrate)
    }

    fun onTerminalResize(columns: Int, rows: Int) {
        _uiState.value = _uiState.value.copy(
            terminalColumns = columns,
            terminalRows = rows,
            selection = null,
            selectedText = ""
        )
        viewModelScope.launch {
            resizeTerminalUseCase(columns, rows)
        }
    }

    fun startSelection(position: TerminalCellPosition) {
        val selection = TerminalSelection(anchor = position)
        _uiState.value = _uiState.value.copy(
            selection = selection,
            selectedText = _uiState.value.rendererState.extractSelectionText(selection)
        )
    }

    fun updateSelection(position: TerminalCellPosition) {
        val currentSelection = _uiState.value.selection ?: return
        val updatedSelection = currentSelection.copy(focus = position)
        _uiState.value = _uiState.value.copy(
            selection = updatedSelection,
            selectedText = _uiState.value.rendererState.extractSelectionText(updatedSelection)
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selection = null,
            selectedText = ""
        )
    }

    fun onSelectionCopied() {
        if (_uiState.value.selectedText.isNotBlank()) {
            _effect.trySend(TerminalUiEffect.ShowToast("Copied terminal selection"))
        }
        clearSelection()
    }

    fun navigateBack() {
        if (_uiState.value.isConnected) {
            _effect.trySend(TerminalUiEffect.ShowDisconnectDialog)
        } else {
            _effect.trySend(TerminalUiEffect.NavigateBack)
        }
    }

    fun confirmDisconnect() {
        disconnect()
        _effect.trySend(TerminalUiEffect.NavigateBack)
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }

    private fun connectCurrentHost() {
        val host = currentHost ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                selection = null,
                selectedText = ""
            )
            val success = startSessionUseCase(
                hostProfile = host,
                onHostKeyUnknown = { algorithm, fingerprint ->
                    waitForHostKeyDecision(algorithm, fingerprint)
                },
                columns = _uiState.value.terminalColumns,
                rows = _uiState.value.terminalRows
            )
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (!success) {
                _effect.trySend(TerminalUiEffect.ShowConnectionError(
                    observeSessionUseCase.getCurrentError() ?: "Connection failed"
                ))
            }
        }
    }

    private suspend fun waitForHostKeyDecision(
        algorithm: String,
        fingerprint: String
    ): HostKeyDecision {
        val deferred = CompletableDeferred<HostKeyDecision>()
        pendingHostKeyDecision = deferred
        _uiState.value = _uiState.value.copy(
            hostKeyPrompt = HostKeyPrompt(algorithm = algorithm, fingerprint = fingerprint)
        )
        return deferred.await()
    }

    private fun updateModifierStates() {
        _uiState.value = _uiState.value.copy(
            activeModifiers = inputController.getActiveModifiers(),
            modifierStates = mapOf(
                ModifierKey.CTRL to inputController.getModifierState(ModifierKey.CTRL),
                ModifierKey.ALT to inputController.getModifierState(ModifierKey.ALT),
                ModifierKey.SHIFT to inputController.getModifierState(ModifierKey.SHIFT)
            )
        )
    }
}
