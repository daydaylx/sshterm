package com.dlx.sshterm.domain.model

/**
 * Known SSH host key entry.
 */
data class KnownHostEntry(
    val host: String,
    val algorithm: String,
    val fingerprint: String,
    val trustDate: Long = System.currentTimeMillis()
) {
    /**
     * Returns the host identifier (hostname:port format).
     */
    fun getHostKey(): String = host

    /**
     * Validates the entry has all required fields.
     */
    fun isValid(): Boolean {
        return host.isNotBlank() &&
            algorithm.isNotBlank() &&
            fingerprint.isNotBlank()
    }

    companion object {
        /**
         * Common SSH key algorithms.
         */
        const val ALGORITHM_SSH_RSA = "ssh-rsa"
        const val ALGORITHM_SSH_ED25519 = "ssh-ed25519"
        const val ALGORITHM_ECDSA_SHA2_NISTP256 = "ecdsa-sha2-nistp256"
        const val ALGORITHM_ECDSA_SHA2_NISTP384 = "ecdsa-sha2-nistp384"
        const val ALGORITHM_ECDSA_SHA2_NISTP521 = "ecdsa-sha2-nistp521"
    }
}
