package com.example.privatessh.ssh.hostkey

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
    private val knownHostRepository: KnownHostRepository
) : HostKeyVerifier {

    private var decisionProvider: ((algorithm: String, fingerprint: String) -> HostKeyDecision)? = null
    private var allowOnlyKnownHosts: Boolean = false

    fun prepare(
        allowOnlyKnownHosts: Boolean = false,
        decisionProvider: ((algorithm: String, fingerprint: String) -> HostKeyDecision)? = null
    ) {
        this.allowOnlyKnownHosts = allowOnlyKnownHosts
        this.decisionProvider = decisionProvider
    }

    fun clear() {
        allowOnlyKnownHosts = false
        decisionProvider = null
    }

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        val host = "$hostname:$port"
        val algorithm = key.algorithm ?: "unknown"
        val fingerprint = formatFingerprint(key)
        val existing = runBlocking { knownHostRepository.getKnownHost(host) }

        if (existing != null && existing.fingerprint == fingerprint) {
            return true
        }

        if (allowOnlyKnownHosts) {
            return false
        }

        val decision = decisionProvider?.invoke(algorithm, fingerprint) ?: HostKeyDecision.Reject
        return when (decision) {
            is HostKeyDecision.TrustAlways -> {
                runBlocking {
                    knownHostRepository.addKnownHost(
                        KnownHostEntry(host = host, algorithm = algorithm, fingerprint = fingerprint)
                    )
                }
                true
            }
            is HostKeyDecision.TrustOnce -> true
            HostKeyDecision.Reject -> false
            is HostKeyDecision.KeyChanged -> false
        }
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()

    private fun formatFingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        val base64 = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$base64"
    }
}
