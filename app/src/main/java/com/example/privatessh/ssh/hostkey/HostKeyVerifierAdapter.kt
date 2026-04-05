package com.example.privatessh.ssh.hostkey

import com.example.privatessh.diagnostics.DiagnosticCategory
import com.example.privatessh.diagnostics.SessionDiagnosticsStore
import com.example.privatessh.domain.model.KnownHostEntry
import com.example.privatessh.domain.repository.KnownHostRepository
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import net.schmizz.sshj.transport.verification.HostKeyVerifier

/**
 * Host key verifier bridged to app-managed trust decisions.
 */
@Singleton
class HostKeyVerifierAdapter @Inject constructor(
    private val knownHostRepository: KnownHostRepository,
    private val diagnosticsStore: SessionDiagnosticsStore
) : HostKeyVerifier {

    @Volatile private var decisionProvider: (suspend (String, String) -> HostKeyDecision)? = null
    @Volatile private var allowOnlyKnownHosts: Boolean = false
    @Volatile private var diagnosticSessionId: String? = null
    @Volatile private var diagnosticHostId: String? = null
    @Volatile private var diagnosticHostName: String? = null

    fun prepare(
        allowOnlyKnownHosts: Boolean = false,
        sessionId: String? = null,
        hostId: String? = null,
        hostName: String? = null,
        decisionProvider: (suspend (String, String) -> HostKeyDecision)? = null
    ) {
        this.allowOnlyKnownHosts = allowOnlyKnownHosts
        diagnosticSessionId = sessionId
        diagnosticHostId = hostId
        diagnosticHostName = hostName
        this.decisionProvider = decisionProvider
    }

    fun clear() {
        allowOnlyKnownHosts = false
        diagnosticSessionId = null
        diagnosticHostId = null
        diagnosticHostName = null
        decisionProvider = null
    }

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val host = "$hostname:$port"
        val algorithm = key.algorithm ?: "unknown"
        val fingerprint = formatFingerprint(key)
        val existing = try {
            runBlocking { knownHostRepository.getKnownHost(host) }
        } catch (_: Exception) { null }

        if (existing != null && existing.fingerprint == fingerprint) {
            return true
        }

        if (existing != null && existing.fingerprint != fingerprint) {
            diagnosticsStore.error(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Schlüssel hat sich geändert",
                detail = "Host: $host\nGespeichert: ${existing.fingerprint}\nEmpfangen: $fingerprint",
                sessionId = diagnosticSessionId,
                hostId = diagnosticHostId,
                hostName = diagnosticHostName ?: host
            )
            return false
        }

        if (allowOnlyKnownHosts) {
            diagnosticsStore.warn(
                category = DiagnosticCategory.HOST_KEY,
                title = "Unbekannter Host-Schlüssel im Reconnect blockiert",
                detail = "Host: $host\nFingerprint: $fingerprint",
                sessionId = diagnosticSessionId,
                hostId = diagnosticHostId,
                hostName = diagnosticHostName ?: host
            )
            return false
        }

        diagnosticsStore.warn(
            category = DiagnosticCategory.HOST_KEY,
            title = "Unbekannter Host-Schlüssel erfordert Bestätigung",
            detail = "Host: $host\nAlgorithmus: $algorithm\nFingerprint: $fingerprint",
            sessionId = diagnosticSessionId,
            hostId = diagnosticHostId,
            hostName = diagnosticHostName ?: host
        )
        val decision = try {
            decisionProvider?.let { provider -> runBlocking { provider(algorithm, fingerprint) } }
                ?: HostKeyDecision.Reject
        } catch (e: Exception) {
            diagnosticsStore.error(
                category = DiagnosticCategory.HOST_KEY,
                title = "Host-Key-Entscheidung fehlgeschlagen",
                throwable = e,
                sessionId = diagnosticSessionId,
                hostId = diagnosticHostId,
                hostName = diagnosticHostName ?: host
            )
            HostKeyDecision.Reject
        }

        return when (decision) {
            is HostKeyDecision.TrustAlways -> {
                try {
                    runBlocking {
                        knownHostRepository.addKnownHost(
                            KnownHostEntry(host = host, algorithm = algorithm, fingerprint = fingerprint)
                        )
                    }
                } catch (_: Exception) { }
                diagnosticsStore.info(
                    category = DiagnosticCategory.HOST_KEY,
                    title = "Host-Schlüssel dauerhaft vertraut",
                    detail = "Host: $host\nFingerprint: $fingerprint",
                    sessionId = diagnosticSessionId,
                    hostId = diagnosticHostId,
                    hostName = diagnosticHostName ?: host
                )
                true
            }
            is HostKeyDecision.TrustOnce -> {
                diagnosticsStore.info(
                    category = DiagnosticCategory.HOST_KEY,
                    title = "Host-Schlüssel einmalig akzeptiert",
                    detail = "Host: $host\nFingerprint: $fingerprint",
                    sessionId = diagnosticSessionId,
                    hostId = diagnosticHostId,
                    hostName = diagnosticHostName ?: host
                )
                true
            }
            HostKeyDecision.Reject -> {
                diagnosticsStore.warn(
                    category = DiagnosticCategory.HOST_KEY,
                    title = "Host-Schlüssel abgelehnt",
                    detail = "Host: $host\nFingerprint: $fingerprint",
                    sessionId = diagnosticSessionId,
                    hostId = diagnosticHostId,
                    hostName = diagnosticHostName ?: host
                )
                false
            }
            is HostKeyDecision.KeyChanged -> {
                diagnosticsStore.error(
                    category = DiagnosticCategory.HOST_KEY,
                    title = "Host-Schlüssel stimmt nicht mehr überein",
                    detail = "Alt: ${decision.oldFingerprint}\nNeu: ${decision.newFingerprint}",
                    sessionId = diagnosticSessionId,
                    hostId = diagnosticHostId,
                    hostName = diagnosticHostName ?: host
                )
                false
            }
        }
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()

    private fun formatFingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        val base64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$base64"
    }
}
