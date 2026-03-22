package com.example.privatessh.ssh.hostkey

/**
 * Decision for host key verification.
 */
sealed class HostKeyDecision {
    /**
     * Trust the host key for this session only.
     */
    data class TrustOnce(val fingerprint: String) : HostKeyDecision()

    /**
     * Trust the host key permanently.
     */
    data class TrustAlways(val fingerprint: String) : HostKeyDecision()

    /**
     * Reject the host key.
     */
    object Reject : HostKeyDecision()

    /**
     * Host key changed - warning to user.
     */
    data class KeyChanged(val oldFingerprint: String, val newFingerprint: String) : HostKeyDecision()
}
