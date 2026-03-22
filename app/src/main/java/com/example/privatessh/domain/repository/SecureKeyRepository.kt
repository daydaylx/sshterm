package com.example.privatessh.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for securely stored SSH private keys.
 */
interface SecureKeyRepository {
    /**
     * Observes all stored key aliases.
     */
    fun observeKeyAliases(): Flow<List<String>>

    /**
     * Stores a private key securely.
     */
    suspend fun storeKey(alias: String, privateKeyPem: String)

    /**
     * Gets a private key by alias.
     */
    suspend fun getKey(alias: String): String?

    /**
     * Deletes a key by alias.
     */
    suspend fun deleteKey(alias: String)

    /**
     * Checks if a key exists.
     */
    suspend fun hasKey(alias: String): Boolean

    /**
     * Gets all available key aliases.
     */
    suspend fun getAllAliases(): List<String>
}
