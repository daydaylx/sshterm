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
import com.example.privatessh.ssh.auth.PasswordAuthStrategy
import com.example.privatessh.ssh.auth.PrivateKeyAuthStrategy
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import com.example.privatessh.terminal.input.InputController
import com.example.privatessh.terminal.input.ModifierKey
import com.example.privatessh.terminal.input.SpecialKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val inputController: InputController,
    private val startSessionUseCase: StartSessionUseCase,
    private val stopSessionUseCase: StopSessionUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val resizeTerminalUseCase: ResizeTerminalUseCase,
    private val getHostByIdUseCase: GetHostByIdUseCase,
    private val passwordAuthStrategy: PasswordAuthStrategy,
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<TerminalUiEffect?>(null)
    val effect = _effect.asStateFlow()

    private var currentHost: HostProfile? = null
    private var pendingHostKeyDecision: CompletableDeferred<HostKeyDecision>? = null

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { sessionState ->
                _uiState.value = _uiState.value.copy(sessionState = sessionState)
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeErrors().collect { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeTerminalOutput().collect { output ->
                _uiState.value = _uiState.value.copy(terminalOutput = output)
            }
        }
        viewModelScope.launch {
            observeSessionUseCase.observeCurrentHost().collect { host ->
                _uiState.value = _uiState.value.copy(hostName = host?.getDisplayName().orEmpty())
            }
        }
        updateModifierStates()
    }

    fun connect(hostId: String) {
        viewModelScope.launch {
            val host = getHostByIdUseCase(hostId)
            if (host == null) {
                _effect.value = TerminalUiEffect.ShowConnectionError("Host not found")
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
                        _effect.value = TerminalUiEffect.ShowConnectionError("No private key stored for this host")
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
        _effect.value = TerminalUiEffect.NavigateBack
    }

    fun onHostKeyDecision(decision: HostKeyDecision) {
        pendingHostKeyDecision?.complete(decision)
        pendingHostKeyDecision = null
        _uiState.value = _uiState.value.copy(hostKeyPrompt = null)
    }

    fun disconnect() {
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
            inputController.handleKeyEvent(event)
            updateModifierStates()
        }
        return true
    }

    fun onSpecialKeyClick(key: SpecialKey) {
        viewModelScope.launch {
            inputController.handleSpecialKey(key)
            _effect.value = TerminalUiEffect.Vibrate
        }
    }

    fun onModifierKeyClick(key: ModifierKey) {
        inputController.handleModifierTap(key)
        updateModifierStates()
        _effect.value = TerminalUiEffect.Vibrate
    }

    fun onTerminalResize(columns: Int, rows: Int) {
        _uiState.value = _uiState.value.copy(terminalColumns = columns, terminalRows = rows)
        viewModelScope.launch {
            resizeTerminalUseCase(columns, rows)
        }
    }

    fun navigateBack() {
        if (_uiState.value.isConnected) {
            _effect.value = TerminalUiEffect.ShowDisconnectDialog
        } else {
            _effect.value = TerminalUiEffect.NavigateBack
        }
    }

    fun confirmDisconnect() {
        disconnect()
        _effect.value = TerminalUiEffect.NavigateBack
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun connectCurrentHost() {
        val host = currentHost ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val success = startSessionUseCase(host) { algorithm, fingerprint ->
                waitForHostKeyDecision(algorithm, fingerprint)
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (!success) {
                _effect.value = TerminalUiEffect.ShowConnectionError(
                    observeSessionUseCase.getCurrentError() ?: "Connection failed"
                )
            }
        }
    }

    private fun waitForHostKeyDecision(
        algorithm: String,
        fingerprint: String
    ): HostKeyDecision {
        val deferred = CompletableDeferred<HostKeyDecision>()
        pendingHostKeyDecision = deferred
        _uiState.value = _uiState.value.copy(
            hostKeyPrompt = HostKeyPrompt(algorithm = algorithm, fingerprint = fingerprint)
        )
        return runBlocking { deferred.await() }
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
