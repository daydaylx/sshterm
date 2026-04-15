package com.dlx.sshterm.presentation.terminal

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlx.sshterm.diagnostics.DiagnosticCategory
import com.dlx.sshterm.diagnostics.DiagnosticEvent
import com.dlx.sshterm.diagnostics.DiagnosticLevel
import com.dlx.sshterm.diagnostics.SessionDiagnosticsStore
import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.usecase.host.GetHostByIdUseCase
import com.dlx.sshterm.domain.usecase.session.ObserveSessionUseCase
import com.dlx.sshterm.domain.usecase.session.ResizeTerminalUseCase
import com.dlx.sshterm.domain.usecase.session.StartSessionUseCase
import com.dlx.sshterm.domain.usecase.session.StopSessionUseCase
import com.dlx.sshterm.domain.usecase.settings.GetSettingsUseCase
import com.dlx.sshterm.ssh.auth.PasswordAuthStrategy
import com.dlx.sshterm.ssh.auth.PrivateKeyAuthStrategy
import com.dlx.sshterm.ssh.hostkey.HostKeyDecision
import com.dlx.sshterm.service.SessionRegistry
import com.dlx.sshterm.terminal.TerminalCellPosition
import com.dlx.sshterm.terminal.TerminalSelection
import com.dlx.sshterm.terminal.extractSelectionText
import com.dlx.sshterm.terminal.input.InputController
import com.dlx.sshterm.terminal.input.ModifierKey
import com.dlx.sshterm.terminal.input.SpecialKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import timber.log.Timber
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
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy,
    private val diagnosticsStore: SessionDiagnosticsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<TerminalUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    @Volatile private var currentHost: HostProfile? = null
    @Volatile private var pendingHostKeyDecision: CompletableDeferred<HostKeyDecision>? = null
    @Volatile private var pendingBiometricAuth: CompletableDeferred<Boolean>? = null
    private var connectJob: kotlinx.coroutines.Job? = null
    private var latestDiagnostics: List<DiagnosticEvent> = emptyList()

    init {
        viewModelScope.launch {
            observeSessionUseCase().collect { sessionState ->
                _uiState.value = _uiState.value.copy(
                    sessionState = sessionState,
                    selection = if (sessionState == com.dlx.sshterm.ssh.SshSessionState.CONNECTED) {
                        _uiState.value.selection
                    } else {
                        null
                    },
                    selectedText = if (sessionState == com.dlx.sshterm.ssh.SshSessionState.CONNECTED) {
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
                if (host != null) {
                    currentHost = host
                }
                _uiState.value = _uiState.value.copy(hostName = host?.getDisplayName().orEmpty())
                updateDiagnosticsState()
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
        viewModelScope.launch {
            getSettingsUseCase.observeBiometricAuthEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(biometricAuthEnabled = enabled)
            }
        }
        viewModelScope.launch {
            diagnosticsStore.events.collect { events ->
                latestDiagnostics = events
                updateDiagnosticsState()
            }
        }
        updateModifierStates()
    }

    fun connect(hostId: String) {
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            val activeHost = observeSessionUseCase.getCurrentHost()
            val currentState = observeSessionUseCase.getCurrentState()
            if (
                activeHost?.id == hostId &&
                currentState in setOf(
                    com.dlx.sshterm.ssh.SshSessionState.CONNECTED,
                    com.dlx.sshterm.ssh.SshSessionState.CONNECTING,
                    com.dlx.sshterm.ssh.SshSessionState.RECONNECTING,
                    com.dlx.sshterm.ssh.SshSessionState.AUTHENTICATING
                )
            ) {
                currentHost = activeHost
                _uiState.value = _uiState.value.copy(hostName = activeHost.getDisplayName(), error = null)
                logInfo(
                    category = DiagnosticCategory.UI,
                    title = "Bereits aktive Sitzung wiederverwendet",
                    host = activeHost,
                    detail = "Die bestehende Terminal-Sitzung wird weiter verwendet."
                )
                updateDiagnosticsState()
                return@launch
            }

            val host = getHostByIdUseCase(hostId)
            if (host == null) {
                diagnosticsStore.error(
                    category = DiagnosticCategory.UI,
                    title = "Host zum Verbinden nicht gefunden",
                    detail = "Host-ID: $hostId",
                    sessionId = hostId,
                    hostId = hostId
                )
                _effect.trySend(TerminalUiEffect.ShowConnectionError("Host not found"))
                return@launch
            }

            currentHost = host
            _uiState.value = _uiState.value.copy(hostName = host.getDisplayName(), error = null)
            updateDiagnosticsState()

            if (_uiState.value.biometricAuthEnabled) {
                val granted = waitForBiometricAuth()
                if (!granted) {
                    logWarning(
                        category = DiagnosticCategory.UI,
                        title = "Biometrische Freigabe abgelehnt",
                        host = host,
                        detail = "Die Verbindung wurde vor dem SSH-Aufbau abgebrochen."
                    )
                    _effect.trySend(TerminalUiEffect.ShowConnectionError("Biometric authentication denied"))
                    return@launch
                }
            }

            when (host.authType) {
                AuthType.PASSWORD -> {
                    if (!passwordAuthStrategy.hasPassword(host.id)) {
                        logWarning(
                            category = DiagnosticCategory.AUTH,
                            title = "Passwort wird vom Benutzer angefordert",
                            host = host,
                            detail = "Es ist kein Passwort im Arbeitsspeicher vorhanden."
                        )
                        _uiState.value = _uiState.value.copy(isAwaitingPassword = true)
                        return@launch
                    }
                }
                AuthType.PRIVATE_KEY -> {
                    if (!privateKeyAuthStrategy.hasPrivateKey(host.id)) {
                        logError(
                            category = DiagnosticCategory.AUTH,
                            title = "Privater Schlüssel fehlt",
                            host = host,
                            detail = "Für diesen Host ist kein gespeicherter Private Key verfügbar."
                        )
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
        logInfo(
            category = DiagnosticCategory.AUTH,
            title = "Passwort übernommen",
            host = host,
            detail = "Die Verbindung wird mit dem eingegebenen Passwort gestartet."
        )
        _uiState.value = _uiState.value.copy(isAwaitingPassword = false)
        connectCurrentHost()
    }

    fun cancelPasswordPrompt() {
        logWarning(
            category = DiagnosticCategory.AUTH,
            title = "Passwortdialog abgebrochen",
            host = currentHost,
            detail = "Die Verbindung wurde ohne Passwort beendet."
        )
        _uiState.value = _uiState.value.copy(isAwaitingPassword = false)
    }

    fun onHostKeyDecision(decision: HostKeyDecision) {
        when (decision) {
            is HostKeyDecision.TrustAlways -> logInfo(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Schlüssel dauerhaft bestätigt",
                host = currentHost,
                detail = decision.fingerprint
            )
            is HostKeyDecision.TrustOnce -> logInfo(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Schlüssel einmalig bestätigt",
                host = currentHost,
                detail = decision.fingerprint
            )
            HostKeyDecision.Reject -> logWarning(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Schlüssel abgelehnt",
                host = currentHost
            )
            is HostKeyDecision.KeyChanged -> logError(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Schlüssel-Konflikt gemeldet",
                host = currentHost,
                detail = "Alt: ${decision.oldFingerprint}\nNeu: ${decision.newFingerprint}"
            )
        }
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
        if (text.isEmpty()) return
        viewModelScope.launch {
            inputController.handleSoftwareInput(text)
            updateModifierStates()
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!inputController.shouldHandleKeyEvent(event)) {
            return false
        }

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
                logError(
                    category = DiagnosticCategory.UI,
                    title = "Verbindungsaufbau fehlgeschlagen",
                    host = host,
                    detail = observeSessionUseCase.getCurrentError() ?: "Connection failed"
                )
                _effect.trySend(TerminalUiEffect.ShowConnectionError(
                    observeSessionUseCase.getCurrentError() ?: "Connection failed"
                ))
            }
        }
    }

    fun onBiometricAuthResult(success: Boolean) {
        pendingBiometricAuth?.complete(success)
        pendingBiometricAuth = null
    }

    private suspend fun waitForBiometricAuth(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        pendingBiometricAuth = deferred
        _effect.trySend(TerminalUiEffect.RequestBiometricAuth)
        return try {
            deferred.await()
        } catch (e: Exception) {
            Timber.w(e, "Biometric auth wait cancelled")
            logWarning(
                category = DiagnosticCategory.UI,
                title = "Biometrische Authentifizierung abgebrochen",
                host = currentHost,
                detail = "Der Biometrie-Dialog wurde nicht erfolgreich abgeschlossen.",
                throwable = e
            )
            false
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
        logWarning(
            category = DiagnosticCategory.HOST_KEY,
            title = "Host-Schlüssel wartet auf Benutzerentscheidung",
            host = currentHost,
            detail = "Algorithmus: $algorithm\nFingerprint: $fingerprint"
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

    private fun updateDiagnosticsState() {
        val hostId = currentHost?.id ?: return
        val visibleDiagnostics = latestDiagnostics
            .filter { it.matches(hostId, hostId) }
            .sortedByDescending { it.id }

        _uiState.value = _uiState.value.copy(
            diagnosticsCount = visibleDiagnostics.size,
            latestDiagnosticError = visibleDiagnostics
                .firstOrNull { it.level == DiagnosticLevel.ERROR }
                ?.title
        )
    }

    private fun logInfo(
        category: DiagnosticCategory,
        title: String,
        host: HostProfile?,
        detail: String? = null
    ) {
        diagnosticsStore.info(
            category = category,
            title = title,
            detail = detail,
            sessionId = host?.id,
            hostId = host?.id,
            hostName = host?.getDisplayName()
        )
    }

    private fun logWarning(
        category: DiagnosticCategory,
        title: String,
        host: HostProfile?,
        detail: String? = null,
        throwable: Throwable? = null
    ) {
        diagnosticsStore.warn(
            category = category,
            title = title,
            detail = detail,
            throwable = throwable,
            sessionId = host?.id,
            hostId = host?.id,
            hostName = host?.getDisplayName()
        )
    }

    private fun logError(
        category: DiagnosticCategory,
        title: String,
        host: HostProfile?,
        detail: String? = null,
        throwable: Throwable? = null
    ) {
        diagnosticsStore.error(
            category = category,
            title = title,
            detail = detail,
            throwable = throwable,
            sessionId = host?.id,
            hostId = host?.id,
            hostName = host?.getDisplayName()
        )
    }
}
