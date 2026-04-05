package com.example.privatessh.domain.usecase.security

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.SecureKeyRepository
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads a private key for SSH authentication.
 */
@ViewModelScoped
class LoadPrivateKeyUseCase @Inject constructor(
    private val secureKeyRepository: SecureKeyRepository
) {

    suspend operator fun invoke(hostProfile: HostProfile): String? {
        val keyAlias = "host_${hostProfile.id}"
        return secureKeyRepository.getKey(keyAlias)
    }

    /**
     * Stores a private key for the given host profile.
     */
    suspend fun storePrivateKey(
        hostProfile: HostProfile,
        privateKeyPem: String
    ): Boolean {
        val keyAlias = "host_${hostProfile.id}"
        return try {
            secureKeyRepository.storeKey(keyAlias, privateKeyPem)
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to store private key for host %s", hostProfile.id)
            false
        }
    }

    /**
     * Checks if a private key exists for the given host profile.
     */
    suspend fun hasPrivateKey(hostProfile: HostProfile): Boolean {
        val keyAlias = "host_${hostProfile.id}"
        return secureKeyRepository.getKey(keyAlias) != null
    }

    /**
     * Removes the private key for the given host profile.
     */
    suspend fun removePrivateKey(hostProfile: HostProfile): Boolean {
        val keyAlias = "host_${hostProfile.id}"
        return try {
            secureKeyRepository.deleteKey(keyAlias)
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to remove private key for host %s", hostProfile.id)
            false
        }
    }
}
