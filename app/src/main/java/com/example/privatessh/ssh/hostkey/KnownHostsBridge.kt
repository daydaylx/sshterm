package com.example.privatessh.ssh.hostkey

import com.example.privatessh.domain.model.KnownHostEntry
import com.example.privatessh.domain.repository.KnownHostRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Bridge between SSHJ's host key verification and the KnownHostRepository.
 */
@ViewModelScoped
class KnownHostsBridge @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) {

    /**
     * Checks if a host is trusted with the given fingerprint.
     */
    suspend fun isTrusted(host: String, fingerprint: String): Boolean {
        return knownHostRepository.isHostTrusted(host, fingerprint)
    }

    /**
     * Adds a known host entry to the repository.
     */
    suspend fun addKnownHost(host: String, algorithm: String, fingerprint: String) {
        val entry = KnownHostEntry(
            host = host,
            algorithm = algorithm,
            fingerprint = fingerprint,
            trustDate = System.currentTimeMillis()
        )
        knownHostRepository.addKnownHost(entry)
    }

    /**
     * Gets the fingerprint for a host (if known).
     */
    suspend fun getKnownFingerprint(host: String): String? {
        return knownHostRepository.getKnownHost(host)?.fingerprint
    }

    /**
     * Gets the algorithm for a host (if known).
     */
    suspend fun getKnownAlgorithm(host: String): String? {
        return knownHostRepository.getKnownHost(host)?.algorithm
    }

    /**
     * Formats the fingerprint for display.
     */
    fun formatForDisplay(fingerprint: String): String {
        // Format like SHA256:abc123... or group by 2 chars
        return fingerprint.chunked(2).joinToString(":")
    }
}
