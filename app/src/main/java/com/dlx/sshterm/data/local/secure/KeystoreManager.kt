package com.dlx.sshterm.data.local.secure

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

private const val AUTH_VALIDITY_SECONDS = 30

/**
 * Manages Android Keystore symmetric keys used to protect SSH private keys at rest.
 */
@Singleton
class KeystoreManager @Inject constructor() {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        const val KEY_ALIAS_PREFIX = "ssh_key_"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    fun getOrCreateSecretKey(alias: String): SecretKey {
        val fullAlias = getFullAlias(alias)
        val existing = keyStore.getKey(fullAlias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                fullAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
                .build()
        )
        return keyGenerator.generateKey()
    }

    fun keyExists(alias: String): Boolean = keyStore.containsAlias(getFullAlias(alias))

    fun deleteKey(alias: String) {
        val fullAlias = getFullAlias(alias)
        if (keyStore.containsAlias(fullAlias)) {
            keyStore.deleteEntry(fullAlias)
        }
    }

    fun listKeyAliases(): List<String> =
        keyStore.aliases().toList()
            .filter { it.startsWith(KEY_ALIAS_PREFIX) }
            .map { it.removePrefix(KEY_ALIAS_PREFIX) }

    private fun getFullAlias(alias: String): String = "$KEY_ALIAS_PREFIX$alias"
}
