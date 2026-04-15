package com.dlx.sshterm.diagnostics

import timber.log.Timber
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac

data class ProbeResult(
    val providers: List<String>,
    val available: List<String>,
    val missing: List<String>
) {
    val allAvailable: Boolean get() = missing.isEmpty()
}

object CryptoProviderProbe {

    private val CHECKS: List<Pair<String, () -> Unit>> = listOf(
        "KeyPairGenerator/RSA" to { KeyPairGenerator.getInstance("RSA") },
        "KeyPairGenerator/EC" to { KeyPairGenerator.getInstance("EC") },
        "KeyAgreement/X25519" to { KeyAgreement.getInstance("X25519") },
        "KeyAgreement/ECDH" to { KeyAgreement.getInstance("ECDH") },
        "Cipher/AES/CTR/NoPadding" to { Cipher.getInstance("AES/CTR/NoPadding") },
        "Cipher/ChaCha20-Poly1305" to { Cipher.getInstance("ChaCha20-Poly1305") },
        "Mac/HmacSHA256" to { Mac.getInstance("HmacSHA256") },
        "Mac/HmacSHA512" to { Mac.getInstance("HmacSHA512") },
        "MessageDigest/SHA-256" to { MessageDigest.getInstance("SHA-256") },
        "MessageDigest/SHA-512" to { MessageDigest.getInstance("SHA-512") },
    )

    fun probe(): ProbeResult {
        val providers = Security.getProviders().map { it.name }
        val available = mutableListOf<String>()
        val missing = mutableListOf<String>()

        for ((name, check) in CHECKS) {
            try {
                check()
                available.add(name)
            } catch (_: Exception) {
                missing.add(name)
            }
        }

        return ProbeResult(providers, available.toList(), missing.toList())
    }

    fun probeAndLog() {
        try {
            val result = probe()
            Timber.d("CryptoProbe providers: %s", result.providers.joinToString())
            Timber.d("CryptoProbe available (%d): %s", result.available.size, result.available.joinToString())
            if (result.missing.isNotEmpty()) {
                Timber.w("CryptoProbe MISSING algorithms: %s", result.missing.joinToString())
            } else {
                Timber.d("CryptoProbe: all required algorithms available")
            }
        } catch (e: Exception) {
            Timber.e(e, "CryptoProbe failed")
        }
    }
}
