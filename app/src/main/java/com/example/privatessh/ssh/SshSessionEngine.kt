package com.example.privatessh.ssh

import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.ssh.auth.PasswordAuthStrategy
import com.example.privatessh.ssh.auth.PrivateKeyAuthStrategy
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import com.example.privatessh.ssh.hostkey.HostKeyVerifierAdapter
import com.example.privatessh.ssh.io.ErrorPump
import com.example.privatessh.ssh.io.InputWriter
import com.example.privatessh.ssh.io.OutputPump
import com.example.privatessh.ssh.io.ShellChannelAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
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
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy
) {

    private val _state = MutableStateFlow(SshSessionState.DISCONNECTED)
    val state: StateFlow<SshSessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _currentHost = MutableStateFlow<HostProfile?>(null)
    val currentHost: StateFlow<HostProfile?> = _currentHost.asStateFlow()

    private var currentClient: SSHClient? = null
    private var currentShellHandle: ShellChannelAdapter.ShellHandle? = null
    private var outputJob: Job? = null
    private var errorJob: Job? = null
    private var sessionScope: CoroutineScope? = null
    private var lastColumns: Int = 80
    private var lastRows: Int = 24

    @Volatile
    private var disconnectRequested = false

    @Volatile
    private var closeHandled = false

    suspend fun connect(
        hostProfile: HostProfile,
        onHostKeyDecision: (algorithm: String, fingerprint: String) -> HostKeyDecision,
        allowOnlyKnownHosts: Boolean = false,
        columns: Int = lastColumns,
        rows: Int = lastRows,
        isReconnect: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        disconnect()

        disconnectRequested = false
        closeHandled = false
        _state.value = if (isReconnect) SshSessionState.RECONNECTING else SshSessionState.CONNECTING
        _error.value = null
        _currentHost.value = hostProfile
        lastColumns = columns.coerceAtLeast(10)
        lastRows = rows.coerceAtLeast(5)

        try {
            val client = sshClientFactory.createClient(hostProfile)
            hostKeyVerifier.prepare(
                allowOnlyKnownHosts = allowOnlyKnownHosts,
                decisionProvider = onHostKeyDecision
            )
            client.addHostKeyVerifier(hostKeyVerifier)
            client.connect(hostProfile.host, hostProfile.port)

            _state.value = SshSessionState.AUTHENTICATING
            val authSuccess = authenticate(client, hostProfile)
            if (!authSuccess) {
                if (hostProfile.authType == AuthType.PASSWORD) {
                    passwordAuthStrategy.clearPassword(hostProfile.id)
                }
                _error.value = "Authentication failed"
                cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
                return@withContext false
            }

            val shellHandle = shellChannelAdapter.openShellChannel(
                client = client,
                terminalType = "xterm-256color",
                columns = lastColumns,
                rows = lastRows
            ) ?: run {
                _error.value = "Failed to open interactive shell"
                cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
                return@withContext false
            }

            currentClient = client
            currentShellHandle = shellHandle
            sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            outputJob = outputPump.start(
                scope = sessionScope!!,
                inputStream = shellHandle.shell.inputStream,
                onOutput = { data ->
                    appendOutput(String(data, Charsets.UTF_8))
                },
                onClosed = { reason ->
                    handleUnexpectedClosure(reason)
                }
            )
            errorJob = errorPump.start(
                scope = sessionScope!!,
                errorStream = shellHandle.shell.errorStream
            ) { data ->
                appendOutput(String(data, Charsets.UTF_8))
            }
            inputWriter.setOutputStream(shellHandle.shell.outputStream)

            _state.value = SshSessionState.CONNECTED
            true
        } catch (e: Exception) {
            _error.value = SshErrorClassifier.classify(e).message
            cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
            false
        } finally {
            hostKeyVerifier.clear()
        }
    }

    suspend fun reconnectLast(): Boolean {
        val hostProfile = _currentHost.value ?: return false
        val capability = canReconnect()
        if (!capability.isAllowed) {
            _error.value = capability.reason ?: "Reconnect is not available for this session."
            _state.value = SshSessionState.ERROR
            return false
        }

        return connect(
            hostProfile = hostProfile,
            onHostKeyDecision = { _, _ -> HostKeyDecision.Reject },
            allowOnlyKnownHosts = true,
            columns = lastColumns,
            rows = lastRows,
            isReconnect = true
        )
    }

    fun canReconnect(): ReconnectCapability {
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
        disconnectRequested = true
        closeHandled = true
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
        currentShellHandle?.let { shellChannelAdapter.resizePty(it, lastColumns, lastRows) }
    }

    fun clearTerminalOutput() {
        _terminalOutput.value = ""
    }

    fun clearSessionSecrets() {
        _currentHost.value?.takeIf { it.authType == AuthType.PASSWORD }?.let { host ->
            passwordAuthStrategy.clearPassword(host.id)
        }
    }

    private fun appendOutput(text: String) {
        _terminalOutput.update { current -> current + text }
    }

    private suspend fun authenticate(client: SSHClient, hostProfile: HostProfile): Boolean {
        val authStrategy = when (hostProfile.authType) {
            AuthType.PASSWORD -> passwordAuthStrategy
            AuthType.PRIVATE_KEY -> privateKeyAuthStrategy
        }

        return try {
            authStrategy.authenticate(client, SshSessionConfig(hostProfile)) != null
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun cleanupSession(
        preserveHost: Boolean,
        finalState: SshSessionState
    ) {
        sessionScope?.cancel()
        joinAll(*listOfNotNull(outputJob, errorJob).toTypedArray())

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

        if (!preserveHost) {
            _currentHost.value = null
        }

        _state.value = finalState
    }

    private fun handleUnexpectedClosure(reason: String?) {
        if (disconnectRequested || closeHandled) {
            return
        }
        closeHandled = true
        sessionScope?.launch {
            _error.value = reason ?: "SSH session closed unexpectedly."
            cleanupSession(preserveHost = true, finalState = SshSessionState.ERROR)
        }
    }
}
