package com.example.privatessh.ssh

import com.example.privatessh.core.constants.Defaults
import com.example.privatessh.diagnostics.DiagnosticCategory
import com.example.privatessh.diagnostics.SessionDiagnosticsStore
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.model.SessionPolicy
import com.example.privatessh.ssh.auth.PasswordAuthStrategy
import com.example.privatessh.ssh.auth.PrivateKeyAuthStrategy
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import com.example.privatessh.ssh.hostkey.HostKeyVerifierAdapter
import com.example.privatessh.ssh.io.ErrorPump
import com.example.privatessh.ssh.io.InputWriter
import com.example.privatessh.ssh.io.OutputPump
import com.example.privatessh.ssh.io.ShellChannelAdapter
import com.example.privatessh.ssh.keepalive.KeepAliveController
import com.example.privatessh.terminal.TerminalEmulator
import com.example.privatessh.terminal.TerminalRendererState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.schmizz.sshj.SSHClient
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class ReconnectCapability(
    val isAllowed: Boolean,
    val passwordCached: Boolean,
    val privateKeyAvailable: Boolean,
    val requiresPasswordPrompt: Boolean,
    val reason: String? = null
)

/**
 * Singleton SSH session engine shared by UI and foreground service.
 */
@Singleton
class SshSessionEngine @Inject constructor(
    private val sshClientFactory: SshClientFactory,
    private val shellChannelAdapter: ShellChannelAdapter,
    private val hostKeyVerifier: HostKeyVerifierAdapter,
    private val outputPump: OutputPump,
    private val errorPump: ErrorPump,
    private val inputWriter: InputWriter,
    private val passwordAuthStrategy: PasswordAuthStrategy,
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy,
    private val keepAliveController: KeepAliveController,
    private val diagnosticsStore: SessionDiagnosticsStore
) {

    private val _state = MutableStateFlow(SshSessionState.DISCONNECTED)
    val state: StateFlow<SshSessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _terminalRendererState = MutableStateFlow(TerminalRendererState.default())
    val terminalRendererState: StateFlow<TerminalRendererState> = _terminalRendererState.asStateFlow()

    private val _currentHost = MutableStateFlow<HostProfile?>(null)
    val currentHost: StateFlow<HostProfile?> = _currentHost.asStateFlow()

    private val _shellStatus = MutableStateFlow(SessionShellStatus())
    val shellStatus: StateFlow<SessionShellStatus> = _shellStatus.asStateFlow()

    private val terminalLock = Any()
    private val terminalEmulator = TerminalEmulator()

    private var currentClient: SSHClient? = null
    private var currentShellHandle: ShellChannelAdapter.ShellHandle? = null
    private var outputJob: Job? = null
    private var errorJob: Job? = null
    private var sessionScope: CoroutineScope? = null
    private var tmuxConfirmationJob: Job? = null
    private var lastColumns: Int = 80
    private var lastRows: Int = 24
    private var lastSessionPolicy: SessionPolicy = SessionPolicy()
    private var lastScrollbackLimit: Int = 2000
    private var tmuxMarkerCarry = ByteArray(0)

    private val disconnectRequested = AtomicBoolean(false)
    private val closeHandled = AtomicBoolean(false)

    suspend fun connect(
        hostProfile: HostProfile,
        onHostKeyDecision: suspend (algorithm: String, fingerprint: String) -> HostKeyDecision,
        allowOnlyKnownHosts: Boolean = false,
        columns: Int = lastColumns,
        rows: Int = lastRows,
        isReconnect: Boolean = false,
        sessionPolicy: SessionPolicy = lastSessionPolicy,
        scrollbackLimit: Int = lastScrollbackLimit
    ): Boolean = withContext(Dispatchers.IO) {
        disconnect()

        disconnectRequested.set(false)
        closeHandled.set(false)
        _state.value = if (isReconnect) SshSessionState.RECONNECTING else SshSessionState.CONNECTING
        _error.value = null
        _currentHost.value = hostProfile
        lastColumns = columns.coerceAtLeast(10)
        lastRows = rows.coerceAtLeast(5)
        lastSessionPolicy = sessionPolicy
        lastScrollbackLimit = scrollbackLimit.coerceAtLeast(100)
        synchronized(terminalLock) { terminalEmulator.setScrollbackLimit(lastScrollbackLimit) }
        resetTerminalState()
        resetShellStatus()
        logInfo(
            category = DiagnosticCategory.CONNECTION,
            title = if (isReconnect) "SSH-Reconnect gestartet" else "SSH-Verbindung gestartet",
            hostProfile = hostProfile,
            detail = buildString {
                append("Ziel: ${hostProfile.host}:${hostProfile.port}")
                append("\nBenutzer: ${hostProfile.user}")
                append("\nAuthentifizierung: ${hostProfile.authType}")
            }
        )

        try {
            val client = sshClientFactory.createClient(hostProfile)
            hostKeyVerifier.prepare(
                allowOnlyKnownHosts = allowOnlyKnownHosts,
                sessionId = hostProfile.id,
                hostId = hostProfile.id,
                hostName = hostProfile.getDisplayName(),
                decisionProvider = onHostKeyDecision
            )
            client.addHostKeyVerifier(hostKeyVerifier)
            client.connect(hostProfile.host, hostProfile.port)
            logInfo(
                category = DiagnosticCategory.CONNECTION,
                title = "SSH-Transport verbunden",
                hostProfile = hostProfile,
                detail = "Socket zu ${hostProfile.host}:${hostProfile.port} aufgebaut."
            )

            _state.value = SshSessionState.AUTHENTICATING
            logInfo(
                category = DiagnosticCategory.AUTH,
                title = "Authentifizierung läuft",
                hostProfile = hostProfile,
                detail = "Benutzer: ${hostProfile.user}\nTyp: ${hostProfile.authType}"
            )
            val authSuccess = authenticate(client, hostProfile)
            if (!authSuccess) {
                if (hostProfile.authType == AuthType.PASSWORD) {
                    passwordAuthStrategy.clearPassword(hostProfile.id)
                }
                logError(
                    category = DiagnosticCategory.AUTH,
                    title = "Authentifizierung fehlgeschlagen",
                    hostProfile = hostProfile,
                    detail = "Die SSH-Authentifizierung konnte nicht abgeschlossen werden."
                )
                _error.value = "Authentication failed"
                cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
                return@withContext false
            }
            logInfo(
                category = DiagnosticCategory.AUTH,
                title = "Authentifizierung erfolgreich",
                hostProfile = hostProfile,
                detail = "Die SSH-Sitzung ist authentifiziert."
            )

            val shellHandle = shellChannelAdapter.openShellChannel(
                client = client,
                sessionId = hostProfile.id,
                hostId = hostProfile.id,
                hostName = hostProfile.getDisplayName(),
                terminalType = "xterm-256color",
                columns = lastColumns,
                rows = lastRows
            ) ?: run {
                logError(
                    category = DiagnosticCategory.SHELL,
                    title = "Interaktive Shell fehlt",
                    hostProfile = hostProfile,
                    detail = "SSH ist verbunden, aber der interaktive Shell-Kanal wurde nicht geöffnet."
                )
                _error.value = "Failed to open interactive shell"
                cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
                return@withContext false
            }
            logInfo(
                category = DiagnosticCategory.SHELL,
                title = "Interaktive Shell geöffnet",
                hostProfile = hostProfile,
                detail = "PTY xterm-256color ${lastColumns}x$lastRows"
            )

            currentClient = client
            currentShellHandle = shellHandle
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            sessionScope = scope

            outputJob = outputPump.start(
                scope = scope,
                inputStream = shellHandle.shell.inputStream,
                onOutput = { data ->
                    appendOutput(data)
                },
                onClosed = { reason ->
                    handleUnexpectedClosure(reason)
                }
            )
            errorJob = errorPump.start(
                scope = scope,
                errorStream = shellHandle.shell.errorStream,
                onClosed = { reason ->
                    reason?.let {
                        logWarning(
                            category = DiagnosticCategory.SHELL,
                            title = "SSH-Error-Stream geschlossen",
                            hostProfile = hostProfile,
                            detail = it
                        )
                    }
                }
            ) { data ->
                appendOutput(data)
            }
            inputWriter.setOutputStream(
                stream = shellHandle.shell.outputStream,
                onError = { e -> handleUnexpectedClosure("SSH write error: ${e.message}") }
            )
            applyShellBootstrap(sessionPolicy)
            keepAliveController.startKeepAlive(
                scope = scope,
                client = client,
                intervalMs = Defaults.KEEPALIVE_INTERVAL_MS,
                onConnectionLost = {
                    logError(
                        category = DiagnosticCategory.KEEPALIVE,
                        title = "Keepalive-Timeout",
                        hostProfile = hostProfile,
                        detail = "Kein Heartbeat mehr empfangen."
                    )
                    handleUnexpectedClosure("No heartbeat — connection timed out")
                },
                onKeepAliveFailure = { throwable ->
                    logWarning(
                        category = DiagnosticCategory.KEEPALIVE,
                        title = "Keepalive-Prüfung fehlgeschlagen",
                        hostProfile = hostProfile,
                        detail = "Die Verbindung konnte nicht mehr geprüft werden.",
                        throwable = throwable
                    )
                }
            )

            _state.value = SshSessionState.CONNECTED
            logInfo(
                category = DiagnosticCategory.CONNECTION,
                title = "SSH-Sitzung verbunden",
                hostProfile = hostProfile,
                detail = "Die interaktive Sitzung ist bereit."
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SshConnect", "SSH connect failed: ${e.javaClass.name}: ${e.message}", e)
            logError(
                category = DiagnosticCategory.CONNECTION,
                title = "SSH-Verbindung fehlgeschlagen",
                hostProfile = hostProfile,
                detail = SshErrorClassifier.classify(e).message,
                throwable = e
            )
            _error.value = SshErrorClassifier.classify(e).message
            cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
            false
        } finally {
            hostKeyVerifier.clear()
        }
    }

    suspend fun reconnectLast(
        sessionPolicy: SessionPolicy = lastSessionPolicy
    ): Boolean {
        val hostProfile = _currentHost.value ?: return false
        val capability = canReconnect()
        if (!capability.isAllowed) {
            logWarning(
                category = DiagnosticCategory.CONNECTION,
                title = "Reconnect nicht möglich",
                hostProfile = hostProfile,
                detail = capability.reason ?: "Reconnect ist für diese Sitzung nicht verfügbar."
            )
            _error.value = capability.reason ?: "Reconnect is not available for this session."
            _state.value = SshSessionState.ERROR
            return false
        }

        logInfo(
            category = DiagnosticCategory.CONNECTION,
            title = "Reconnect angefordert",
            hostProfile = hostProfile,
            detail = "Die letzte Sitzung wird erneut aufgebaut."
        )
        return connect(
            hostProfile = hostProfile,
            onHostKeyDecision = { _, _ -> HostKeyDecision.Reject },
            allowOnlyKnownHosts = true,
            columns = lastColumns,
            rows = lastRows,
            isReconnect = true,
            sessionPolicy = sessionPolicy
        )
    }

    suspend fun canReconnect(): ReconnectCapability {
        val hostProfile = _currentHost.value ?: return ReconnectCapability(
            isAllowed = false,
            passwordCached = false,
            privateKeyAvailable = false,
            requiresPasswordPrompt = false,
            reason = "No previous session is available."
        )

        return when (hostProfile.authType) {
            AuthType.PASSWORD -> {
                val passwordCached = passwordAuthStrategy.hasPassword(hostProfile.id)
                ReconnectCapability(
                    isAllowed = passwordCached,
                    passwordCached = passwordCached,
                    privateKeyAvailable = false,
                    requiresPasswordPrompt = !passwordCached,
                    reason = if (passwordCached) null else "Password is no longer available in memory."
                )
            }

            AuthType.PRIVATE_KEY -> {
                val privateKeyAvailable = privateKeyAuthStrategy.hasPrivateKey(hostProfile.id)
                ReconnectCapability(
                    isAllowed = privateKeyAvailable,
                    passwordCached = false,
                    privateKeyAvailable = privateKeyAvailable,
                    requiresPasswordPrompt = false,
                    reason = if (privateKeyAvailable) null else "Private key is not available for reconnect."
                )
            }
        }
    }

    suspend fun disconnect(preserveHost: Boolean = false) {
        disconnectRequested.set(true)
        closeHandled.set(true)
        if (_state.value != SshSessionState.DISCONNECTED || _currentHost.value != null) {
            logInfo(
                category = DiagnosticCategory.CONNECTION,
                title = "Disconnect angefordert",
                hostProfile = _currentHost.value,
                detail = "Die SSH-Sitzung wird beendet."
            )
        }
        _error.value = null

        if (_state.value != SshSessionState.DISCONNECTED) {
            _state.value = SshSessionState.DISCONNECTING
        }

        cleanupSession(preserveHost = preserveHost, finalState = SshSessionState.DISCONNECTED)
    }

    suspend fun sendInput(data: ByteArray) {
        inputWriter.write(data)
    }

    suspend fun resizeTerminal(columns: Int, rows: Int) {
        lastColumns = columns.coerceAtLeast(10)
        lastRows = rows.coerceAtLeast(5)
        synchronized(terminalLock) {
            _terminalRendererState.value = terminalEmulator.resize(lastColumns, lastRows)
        }
        currentShellHandle?.let { shellChannelAdapter.resizePty(it, lastColumns, lastRows) }
    }

    fun clearTerminalOutput() {
        resetTerminalState()
    }

    fun clearSessionSecrets() {
        _currentHost.value?.takeIf { it.authType == AuthType.PASSWORD }?.let { host ->
            passwordAuthStrategy.clearPassword(host.id)
        }
    }

    private fun resetTerminalState() {
        synchronized(terminalLock) {
            _terminalRendererState.value = terminalEmulator.reset(lastColumns, lastRows)
        }
    }

    private fun resetShellStatus() {
        tmuxConfirmationJob?.cancel()
        tmuxConfirmationJob = null
        tmuxMarkerCarry = byteArrayOf()
        _shellStatus.value = SessionShellStatus()
    }

    private fun appendOutput(data: ByteArray) {
        val sanitizedData = processBootstrapMarkers(data)
        if (sanitizedData.isEmpty()) {
            return
        }

        synchronized(terminalLock) {
            _terminalRendererState.value = terminalEmulator.feed(sanitizedData)
        }
    }

    private suspend fun applyShellBootstrap(sessionPolicy: SessionPolicy) {
        val bootstrapPlan = ShellBootstrapPlanner.buildPlan(sessionPolicy)
        _shellStatus.value = bootstrapPlan.initialStatus

        val command = bootstrapPlan.command ?: return
        logInfo(
            category = DiagnosticCategory.SHELL,
            title = bootstrapPlan.initialStatus.message ?: "Shell-Bootstrap angefordert",
            hostProfile = _currentHost.value,
            detail = "Beim Verbinden wird automatisch ein Shell-Bootstrap ausgeführt."
        )
        inputWriter.writeString(command)
        tmuxConfirmationJob?.cancel()
        tmuxConfirmationJob = sessionScope?.launch {
            delay(750)
            if (_shellStatus.value.mode == SessionShellMode.TMUX_REQUESTED && _state.value == SshSessionState.CONNECTED) {
                _shellStatus.value = SessionShellStatus(
                    mode = SessionShellMode.TMUX_ATTACHED,
                    message = "tmux session active"
                )
                logInfo(
                    category = DiagnosticCategory.SHELL,
                    title = "tmux-Sitzung aktiv",
                    hostProfile = _currentHost.value,
                    detail = "Die Shell wurde an eine tmux-Sitzung angehängt."
                )
            }
        }
    }

    private fun processBootstrapMarkers(data: ByteArray): ByteArray {
        val markerBytes = ShellBootstrapPlanner.TMUX_FALLBACK_MARKER.encodeToByteArray()
        val combined = tmuxMarkerCarry + data
        if (combined.isEmpty()) {
            return combined
        }

        val output = ArrayList<Byte>(combined.size)
        var index = 0
        while (index < combined.size) {
            if (combined.matchesAt(index, markerBytes)) {
                tmuxConfirmationJob?.cancel()
                _shellStatus.value = SessionShellStatus(
                    mode = SessionShellMode.TMUX_FALLBACK,
                    message = "tmux unavailable, using shell"
                )
                logWarning(
                    category = DiagnosticCategory.SHELL,
                    title = "tmux nicht verfügbar",
                    hostProfile = _currentHost.value,
                    detail = "Es wird auf eine normale Shell zurückgefallen."
                )
                index += markerBytes.size
                while (index < combined.size && combined[index] in listOf('\r'.code.toByte(), '\n'.code.toByte())) {
                    index++
                }
                continue
            }

            val tail = combined.copyOfRange(index, combined.size)
            if (markerBytes.startsWith(tail) && tail.size < markerBytes.size) {
                break
            }

            output += combined[index]
            index++
        }

        tmuxMarkerCarry = if (index < combined.size) {
            combined.copyOfRange(index, combined.size)
        } else {
            byteArrayOf()
        }

        return output.toByteArray()
    }

    private suspend fun authenticate(client: SSHClient, hostProfile: HostProfile): Boolean {
        val authStrategy = when (hostProfile.authType) {
            AuthType.PASSWORD -> passwordAuthStrategy
            AuthType.PRIVATE_KEY -> privateKeyAuthStrategy
        }

        return try {
            authStrategy.authenticate(client, SshSessionConfig(hostProfile)) != null
        } catch (e: Exception) {
            logError(
                category = DiagnosticCategory.AUTH,
                title = "Authentifizierung mit Exception abgebrochen",
                hostProfile = hostProfile,
                detail = "Die Authentifizierungsstrategie hat eine Exception geworfen.",
                throwable = e
            )
            false
        }
    }

    private suspend fun cleanupSession(
        preserveHost: Boolean,
        finalState: SshSessionState
    ) {
        keepAliveController.stopKeepAlive()
        tmuxConfirmationJob?.cancel()
        tmuxConfirmationJob = null
        sessionScope?.cancel()
        withTimeoutOrNull(5_000) {
            joinAll(*listOfNotNull(outputJob, errorJob).toTypedArray())
        }

        currentShellHandle?.let { shellChannelAdapter.closeChannel(it) }

        try {
            currentClient?.disconnect()
        } catch (_: Exception) {
        }

        currentClient = null
        currentShellHandle = null
        outputJob = null
        errorJob = null
        sessionScope = null
        inputWriter.clear()
        hostKeyVerifier.clear()
        tmuxMarkerCarry = byteArrayOf()
        _shellStatus.value = SessionShellStatus()

        if (!preserveHost) {
            _currentHost.value = null
        }

        _state.value = finalState
    }

    private fun handleUnexpectedClosure(reason: String?) {
        if (disconnectRequested.get()) return
        if (!closeHandled.compareAndSet(false, true)) return
        val scope = sessionScope ?: return
        scope.launch {
            logError(
                category = DiagnosticCategory.CONNECTION,
                title = "SSH-Sitzung unerwartet beendet",
                hostProfile = _currentHost.value,
                detail = reason ?: "SSH session closed unexpectedly."
            )
            _error.value = reason ?: "SSH session closed unexpectedly."
            cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
        }
    }

    private fun logInfo(
        category: DiagnosticCategory,
        title: String,
        hostProfile: HostProfile?,
        detail: String? = null
    ) {
        diagnosticsStore.info(
            category = category,
            title = title,
            detail = detail,
            sessionId = hostProfile?.id,
            hostId = hostProfile?.id,
            hostName = hostProfile?.getDisplayName()
        )
    }

    private fun logWarning(
        category: DiagnosticCategory,
        title: String,
        hostProfile: HostProfile?,
        detail: String? = null,
        throwable: Throwable? = null
    ) {
        diagnosticsStore.warn(
            category = category,
            title = title,
            detail = detail,
            throwable = throwable,
            sessionId = hostProfile?.id,
            hostId = hostProfile?.id,
            hostName = hostProfile?.getDisplayName()
        )
    }

    private fun logError(
        category: DiagnosticCategory,
        title: String,
        hostProfile: HostProfile?,
        detail: String? = null,
        throwable: Throwable? = null
    ) {
        diagnosticsStore.error(
            category = category,
            title = title,
            detail = detail,
            throwable = throwable,
            sessionId = hostProfile?.id,
            hostId = hostProfile?.id,
            hostName = hostProfile?.getDisplayName()
        )
    }

    private fun ByteArray.matchesAt(startIndex: Int, candidate: ByteArray): Boolean {
        if (startIndex + candidate.size > size) {
            return false
        }

        for (offset in candidate.indices) {
            if (this[startIndex + offset] != candidate[offset]) {
                return false
            }
        }
        return true
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (prefix.size > size) {
            return false
        }

        for (index in prefix.indices) {
            if (this[index] != prefix[index]) {
                return false
            }
        }
        return true
    }
}
