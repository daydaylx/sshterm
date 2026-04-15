package com.dlx.sshterm.ssh

/**
 * Classifies SSH connection errors.
 */
object SshErrorClassifier {

    /**
     * Classifies an exception into a user-friendly error message.
     */
    fun classify(throwable: Throwable): SshError {
        val message = throwable.message ?: throwable.javaClass.simpleName

        return when {
            // Connection errors
            throwable is java.net.ConnectException ||
            throwable is java.net.SocketTimeoutException ||
            message.contains("Connection refused") ||
            message.contains("Connection timed out") -> {
                SshError.ConnectionRefused(
                    message = "Could not connect to host. Check if the host is reachable and the SSH port is open."
                )
            }

            // Host key verification errors
            message.contains("Host key verification failed") -> {
                SshError.HostKeyVerificationFailed(
                    message = "Host key verification failed."
                )
            }

            // Authentication errors
            message.contains("Auth failed") ||
            message.contains("Authentication failed") -> {
                SshError.AuthenticationFailed(
                    message = "Authentication failed. Check your credentials."
                )
            }

            // Unknown host
            message.contains("UnknownHostException") ||
            message.contains("Unable to resolve hostname") -> {
                SshError.UnknownHost(
                    message = "Unknown host. Check the hostname or try using an IP address."
                )
            }

            // Network unreachable
            message.contains("Network is unreachable") -> {
                SshError.NetworkUnreachable(
                    message = "Network is unreachable. Check your internet connection."
                )
            }

            // Generic SSH error
            else -> SshError.Generic(
                message = message
            )
        }
    }
}

/**
 * SSH error types.
 */
sealed class SshError {
    abstract val message: String

    data class ConnectionRefused(override val message: String) : SshError()
    data class HostKeyVerificationFailed(override val message: String) : SshError()
    data class AuthenticationFailed(override val message: String) : SshError()
    data class UnknownHost(override val message: String) : SshError()
    data class NetworkUnreachable(override val message: String) : SshError()
    data class Generic(override val message: String) : SshError()
}
