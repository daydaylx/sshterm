package com.example.privatessh.domain.usecase.network

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Checks if an SSH target is reachable on the network.
 */
@ViewModelScoped
class CheckReachabilityUseCase @Inject constructor() {

    /**
     * Checks if a host:port is reachable.
     * This performs a basic TCP connection test.
     */
    suspend operator fun invoke(
        hostname: String,
        port: Int,
        timeoutMs: Long = 5000
    ): ReachabilityResult = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(hostname, port), timeoutMs.toInt())
                ReachabilityResult.Reachable(hostname, port)
            }
        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException -> {
                    ReachabilityResult.DnsFailed(hostname)
                }
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> {
                    ReachabilityResult.ConnectionRefused(hostname, port)
                }
                else -> {
                    ReachabilityResult.Unreachable(hostname, port, e.message)
                }
            }
        }
    }

    /**
     * Checks if Tailscale daemon is running (for Tailscale targets).
     */
    suspend fun checkTailscaleRunning(): Boolean {
        return try {
            // Try to connect to Tailscale's local API
            java.net.URL("http://100.100.100.100").openStream().close()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Result of a reachability check.
 */
sealed class ReachabilityResult {
    /**
     * Host is reachable.
     */
    data class Reachable(val hostname: String, val port: Int) : ReachabilityResult()

    /**
     * DNS resolution failed.
     */
    data class DnsFailed(val hostname: String) : ReachabilityResult()

    /**
     * Connection refused - port is closed.
     */
    data class ConnectionRefused(val hostname: String, val port: Int) : ReachabilityResult()

    /**
     * Host is unreachable.
     */
    data class Unreachable(
        val hostname: String,
        val port: Int,
        val reason: String?
    ) : ReachabilityResult()
}
