package com.dlx.sshterm.data.local.secure

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores SSH private keys encrypted with a per-alias Android Keystore key.
 */
private val Context.secureKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_keys")

@Singleton
class SecureKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keystoreManager: KeystoreManager
) {

    private object Keys {
        val ALIASES = stringSetPreferencesKey("secure_key_aliases")
    }

    fun observeKeyAliases(): Flow<List<String>> =
        context.secureKeyDataStore.data.map { preferences ->
            (preferences[Keys.ALIASES] ?: emptySet()).toList().sorted()
        }

    suspend fun storeKey(alias: String, privateKeyPem: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreManager.getOrCreateSecretKey(alias))
        val encryptedBytes = cipher.doFinal(privateKeyPem.toByteArray(StandardCharsets.UTF_8))
        val payload = encode(cipher.iv) + SEPARATOR + encode(encryptedBytes)

        context.secureKeyDataStore.edit { preferences ->
            preferences[keyForAlias(alias)] = payload
            val aliases = preferences[Keys.ALIASES].orEmpty().toMutableSet()
            aliases += alias
            preferences[Keys.ALIASES] = aliases
        }
    }

    suspend fun getKey(alias: String): String? {
        val payload = context.secureKeyDataStore.data.first()[keyForAlias(alias)] ?: return null
        val parts = payload.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            keystoreManager.getOrCreateSecretKey(alias),
            GCMParameterSpec(TAG_LENGTH_BITS, decode(parts[0]))
        )

        return cipher.doFinal(decode(parts[1])).toString(StandardCharsets.UTF_8)
    }

    suspend fun deleteKey(alias: String) {
        context.secureKeyDataStore.edit { preferences ->
            preferences.remove(keyForAlias(alias))
            val aliases = preferences[Keys.ALIASES].orEmpty().toMutableSet()
            aliases -= alias
            preferences[Keys.ALIASES] = aliases
        }
        keystoreManager.deleteKey(alias)
    }

    suspend fun hasKey(alias: String): Boolean =
        context.secureKeyDataStore.data.first().contains(keyForAlias(alias))

    suspend fun getAllAliases(): List<String> = observeKeyAliases().first()

    private fun keyForAlias(alias: String) = stringPreferencesKey("encrypted_key_$alias")

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val SEPARATOR = ":"
    }
}
