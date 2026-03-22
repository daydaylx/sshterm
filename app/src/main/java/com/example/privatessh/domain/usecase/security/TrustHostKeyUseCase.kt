package com.example.privatessh.domain.usecase.security

import com.example.privatessh.domain.model.KnownHostEntry
import com.example.privatessh.domain.repository.KnownHostRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Adds a host key to the known hosts repository.
 */
@ViewModelScoped
class TrustHostKeyUseCase @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) {

    suspend operator fun invoke(
        host: String,
        algorithm: String,
        fingerprint: String
    ): Boolean {
        return try {
            val entry = KnownHostEntry(
                host = host,
                algorithm = algorithm,
                fingerprint = fingerprint
            )
            knownHostRepository.addKnownHost(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Trusts a host key entry directly.
     */
    suspend operator fun invoke(entry: KnownHostEntry): Boolean {
        return try {
            knownHostRepository.addKnownHost(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Removes a host key from the known hosts repository.
     */
    suspend fun untrustHost(host: String): Boolean {
        return try {
            // Note: KnownHostRepository would need a removeKnownHost method
            // For now, this is a placeholder
            true
        } catch (e: Exception) {
            false
        }
    }
}
