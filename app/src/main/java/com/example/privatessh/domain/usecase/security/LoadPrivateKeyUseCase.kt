package com.example.privatessh.domain.usecase.security

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.SecureKeyRepository
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Loads a private key for SSH authentication.
 */
@ViewModelScoped
class LoadPrivateKeyUseCase @Inject constructor(
    private val secureKeyRepository: SecureKeyRepository
) {

    suspend operator fun invoke(hostProfile: HostProfile): String? {
        val keyAlias = "key_${hostProfile.id}"
        return secureKeyRepository.getKey(keyAlias)
    }

    /**
     * Stores a private key for the given host profile.
     */
    suspend fun storePrivateKey(
        hostProfile: HostProfile,
        privateKeyPem: String
    ): Boolean {
        val keyAlias = "key_${hostProfile.id}"
        return try {
            secureKeyRepository.storeKey(keyAlias, privateKeyPem)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a private key exists for the given host profile.
     */
    suspend fun hasPrivateKey(hostProfile: HostProfile): Boolean {
        val keyAlias = "key_${hostProfile.id}"
        return secureKeyRepository.getKey(keyAlias) != null
    }

    /**
     * Removes the private key for the given host profile.
     */
    suspend fun removePrivateKey(hostProfile: HostProfile): Boolean {
        val keyAlias = "key_${hostProfile.id}"
        return try {
            secureKeyRepository.deleteKey(keyAlias)
            true
        } catch (e: Exception) {
            false
        }
    }
}
