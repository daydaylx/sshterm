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
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun connect(
        hostProfile: HostProfile,
        onHostKeyDecision: (algorithm: String, fingerprint: String) -> HostKeyDecision,
        allowOnlyKnownHosts: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        disconnect()

        _state.value = SshSessionState.CONNECTING
        _error.value = null
        _currentHost.value = hostProfile

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
                _state.value = SshSessionState.ERROR
                _error.value = "Authentication failed"
                disconnect(preserveHost = true)
                return@withContext false
            }

            val shellHandle = shellChannelAdapter.openShellChannel(
                client = client,
                terminalType = "xterm-256color",
                columns = 80,
                rows = 24
            ) ?: run {
                _state.value = SshSessionState.ERROR
                _error.value = "Failed to open interactive shell"
                disconnect(preserveHost = true)
                return@withContext false
            }

            currentClient = client
            currentShellHandle = shellHandle
            sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            outputJob = outputPump.start(
                scope = sessionScope!!,
                inputStream = shellHandle.shell.inputStream
            ) { data ->
                appendOutput(String(data, Charsets.UTF_8))
            }
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
            _state.value = SshSessionState.ERROR
            _error.value = SshErrorClassifier.classify(e).message
            disconnect(preserveHost = true)
            false
        } finally {
            hostKeyVerifier.clear()
        }
    }

    suspend fun reconnectLast(): Boolean {
        val hostProfile = _currentHost.value ?: return false
        return connect(
            hostProfile = hostProfile,
            onHostKeyDecision = { _, fingerprint -> HostKeyDecision.TrustOnce(fingerprint) },
            allowOnlyKnownHosts = true
        )
    }

    suspend fun disconnect(preserveHost: Boolean = false) {
        _state.value = SshSessionState.DISCONNECTING

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

        _state.value = SshSessionState.DISCONNECTED
    }

    suspend fun sendInput(data: ByteArray) {
        inputWriter.write(data)
    }

    suspend fun resizeTerminal(columns: Int, rows: Int) {
        currentShellHandle?.let { shellChannelAdapter.resizePty(it, columns, rows) }
    }

    fun clearTerminalOutput() {
        _terminalOutput.value = ""
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
}
