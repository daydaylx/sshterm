package com.dlx.sshterm.domain.usecase.security

import com.dlx.sshterm.domain.model.KnownHostEntry
import com.dlx.sshterm.domain.repository.KnownHostRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Verifies an SSH host key against the known hosts repository.
 */
@ViewModelScoped
class VerifyHostKeyUseCase @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) {

    suspend operator fun invoke(
        host: String,
        algorithm: String,
        fingerprint: String
    ): VerifyHostKeyResult {
        val knownHost = knownHostRepository.getKnownHost(host)

        return when {
            knownHost == null -> {
                // First time connecting to this host
                VerifyHostKeyResult.UnknownHost(algorithm, fingerprint)
            }
            knownHost.fingerprint == fingerprint -> {
                // Host key matches
                VerifyHostKeyResult.Trusted
            }
            else -> {
                // Host key has changed!
                VerifyHostKeyResult.KeyChanged(
                    previousAlgorithm = knownHost.algorithm,
                    previousFingerprint = knownHost.fingerprint,
                    newAlgorithm = algorithm,
                    newFingerprint = fingerprint
                )
            }
        }
    }

    /**
     * Verifies a host key entry directly.
     */
    suspend operator fun invoke(entry: KnownHostEntry): VerifyHostKeyResult {
        return invoke(entry.host, entry.algorithm, entry.fingerprint)
    }
}

/**
 * Result of host key verification.
 */
sealed class VerifyHostKeyResult {
    /**
     * The host is not in the known hosts database.
     */
    data class UnknownHost(val algorithm: String, val fingerprint: String) : VerifyHostKeyResult()

    /**
     * The host key matches the stored key.
     */
    data object Trusted : VerifyHostKeyResult()

    /**
     * The host key has changed since the last connection.
     */
    data class KeyChanged(
        val previousAlgorithm: String,
        val previousFingerprint: String,
        val newAlgorithm: String,
        val newFingerprint: String
    ) : VerifyHostKeyResult()
}
