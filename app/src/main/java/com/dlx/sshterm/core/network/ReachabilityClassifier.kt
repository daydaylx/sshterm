package com.dlx.sshterm.core.network

/**
 * Classification of SSH connection failures.
 * Provides detailed error messages for different failure scenarios.
 */
sealed class ConnectionFailure {
    /**
     * DNS resolution failed - hostname not found.
     */
    data class DnsFailed(val hostname: String) : ConnectionFailure()

    /**
     * Connection refused - host is not listening on the SSH port.
     */
    data class ConnectionRefused(val hostname: String, val port: Int) : ConnectionFailure()

    /**
     * Connection timed out - no response from host.
     */
    data class ConnectionTimeout(val hostname: String, val port: Int) : ConnectionFailure()

    /**
     * Network unreachable - no network path to host.
     */
    data class NetworkUnreachable(val hostname: String) : ConnectionFailure()

    /**
     * Authentication failed - incorrect credentials.
     */
    data class AuthenticationFailed(val hostname: String) : ConnectionFailure()

    /**
     * Host key verification failed.
     */
    data class HostKeyVerificationFailed(
        val hostname: String,
        val expectedFingerprint: String,
        val actualFingerprint: String
    ) : ConnectionFailure()

    /**
     * Tailscale-specific error - Tailscale might not be running.
     */
    data class TailscaleNotRunning(
        val hostname: String
    ) : ConnectionFailure()

    /**
     * Tailscale-specific error - host not found in tailnet.
     */
    data class TailscaleHostNotFound(
        val hostname: String
    ) : ConnectionFailure()

    /**
     * Generic connection error.
     */
    data class GenericError(
        val hostname: String,
        val message: String
    ) : ConnectionFailure()

    /**
     * Returns a user-friendly error message.
     */
    fun getErrorMessage(): String {
        return when (this) {
            is DnsFailed -> "Cannot resolve hostname '${this.hostname}'"
            is ConnectionRefused -> "Connection refused - SSH port ${this.port} is closed on '${this.hostname}'"
            is ConnectionTimeout -> "Connection timeout - no response from '${this.hostname}:${this.port}'"
            is NetworkUnreachable -> "Network unreachable - cannot connect to '${this.hostname}'"
            is AuthenticationFailed -> "Authentication failed - check your credentials for '${this.hostname}'"
            is HostKeyVerificationFailed -> "Host key verification failed - host key has changed!"
            is TailscaleNotRunning -> "Tailscale may not be running - check your Tailscale connection"
            is TailscaleHostNotFound -> "Host '${this.hostname}' not found in your tailnet"
            is GenericError -> "Connection error: ${this.message}"
        }
    }

    /**
     * Returns a suggested action for the user.
     */
    fun getSuggestedAction(): String {
        return when (this) {
            is DnsFailed -> "Check the hostname and your network connection"
            is ConnectionRefused -> "Ensure SSH service is running on the host"
            is ConnectionTimeout -> "Check if the host is online and not blocking connections"
            is NetworkUnreachable -> "Check your internet connection"
            is AuthenticationFailed -> "Verify your username/password or SSH key"
            is HostKeyVerificationFailed -> "WARNING: Security risk - host key may have changed"
            is TailscaleNotRunning -> "Open Tailscale and ensure you're logged in"
            is TailscaleHostNotFound -> "Verify the hostname and check your tailnet access"
            is GenericError -> "Try again or check your network settings"
        }
    }

    /**
     * Returns true if this is a Tailscale-specific error.
     */
    fun isTailscaleError(): Boolean {
        return this is TailscaleNotRunning || this is TailscaleHostNotFound
    }

    /**
     * Returns true if the error might be related to battery optimizations.
     */
    fun isBatteryOptimizationIssue(): Boolean {
        return this is ConnectionTimeout || this is NetworkUnreachable
    }
}
