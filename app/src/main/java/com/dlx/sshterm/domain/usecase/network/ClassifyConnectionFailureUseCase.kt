package com.dlx.sshterm.domain.usecase.network

import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import com.dlx.sshterm.core.network.ConnectionFailure
import com.dlx.sshterm.core.network.NetworkType
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Classifies connection failures based on exception type and context.
 */
@ViewModelScoped
class ClassifyConnectionFailureUseCase @Inject constructor(
    private val networkTypeUseCase: ResolveTargetUseCase,
    private val dispatchers: DispatcherProvider
) {

    /**
     * Classifies a connection failure into a detailed error type.
     */
    suspend operator fun invoke(
        hostname: String,
        port: Int,
        exception: Throwable,
        authType: String
    ): ConnectionFailure = withContext(dispatchers.default) {
        val networkType = networkTypeUseCase(hostname)
        val errorMessage = exception.message?.lowercase() ?: ""
        val exceptionType = exception::class.simpleName ?: ""

        when {
            // DNS resolution failures
            errorMessage.contains("unknown host") ||
            errorMessage.contains("hostname") ||
            errorMessage.contains("dns") ||
            exceptionType == "UnknownHostException" -> {
                if (networkType.isTailscale()) {
                    ConnectionFailure.TailscaleHostNotFound(hostname)
                } else {
                    ConnectionFailure.DnsFailed(hostname)
                }
            }

            // Connection refused
            errorMessage.contains("connection refused") ||
            errorMessage.contains("port") && errorMessage.contains("closed") -> {
                ConnectionFailure.ConnectionRefused(hostname, port)
            }

            // Connection timeout
            errorMessage.contains("timed out") ||
            errorMessage.contains("timeout") ||
            exceptionType == "SocketTimeoutException" -> {
                ConnectionFailure.ConnectionTimeout(hostname, port)
            }

            // Network unreachable
            errorMessage.contains("unreachable") ||
            errorMessage.contains("no route") ||
            errorMessage.contains("network") -> {
                if (networkType.isTailscale()) {
                    ConnectionFailure.TailscaleNotRunning(hostname)
                } else {
                    ConnectionFailure.NetworkUnreachable(hostname)
                }
            }

            // Authentication failures
            errorMessage.contains("auth") ||
            errorMessage.contains("password") ||
            errorMessage.contains("credential") -> {
                ConnectionFailure.AuthenticationFailed(hostname)
            }

            // Host key verification failures
            errorMessage.contains("host key") ||
            errorMessage.contains("fingerprint") -> {
                ConnectionFailure.HostKeyVerificationFailed(
                    hostname,
                    expectedFingerprint = "",
                    actualFingerprint = exception.message ?: ""
                )
            }

            // Generic error
            else -> ConnectionFailure.GenericError(
                hostname,
                errorMessage
            )
        }
    }

    /**
     * Creates a user-friendly summary of the connection failure.
     */
    fun getSummary(failure: ConnectionFailure): String {
        return buildString {
            append(failure.getErrorMessage())
            append("\n\n")
            append("Suggestion: ")
            append(failure.getSuggestedAction())
        }
    }
}
