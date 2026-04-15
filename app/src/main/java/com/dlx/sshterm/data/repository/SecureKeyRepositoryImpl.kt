package com.dlx.sshterm.data.repository

import com.dlx.sshterm.data.local.secure.SecureKeyStorage
import com.dlx.sshterm.domain.repository.SecureKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for secure SSH key storage.
 */
class SecureKeyRepositoryImpl @Inject constructor(
    private val secureKeyStorage: SecureKeyStorage
) : SecureKeyRepository {

    override fun observeKeyAliases(): Flow<List<String>> =
        secureKeyStorage.observeKeyAliases()

    override suspend fun storeKey(alias: String, privateKeyPem: String) {
        secureKeyStorage.storeKey(alias, privateKeyPem)
    }

    override suspend fun getKey(alias: String): String? =
        secureKeyStorage.getKey(alias)

    override suspend fun deleteKey(alias: String) {
        secureKeyStorage.deleteKey(alias)
    }

    override suspend fun hasKey(alias: String): Boolean =
        secureKeyStorage.hasKey(alias)

    override suspend fun getAllAliases(): List<String> =
        secureKeyStorage.getAllAliases()
}
