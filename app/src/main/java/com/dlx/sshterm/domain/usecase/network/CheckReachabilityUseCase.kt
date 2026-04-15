package com.dlx.sshterm.domain.usecase.network

import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Checks if an SSH target is reachable on the network.
 */
@ViewModelScoped
class CheckReachabilityUseCase @Inject constructor(
    private val dispatchers: DispatcherProvider
) {

    /**
     * Checks if a host:port is reachable.
     * This performs a basic TCP connection test.
     */
    suspend operator fun invoke(
        hostname: String,
        port: Int,
        timeoutMs: Long = 5000
    ): ReachabilityResult = withContext(dispatchers.io) {
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
}

/**
 * Result of a reachability check.
 */
sealed class ReachabilityResult {
    data class Reachable(val hostname: String, val port: Int) : ReachabilityResult()
    data class DnsFailed(val hostname: String) : ReachabilityResult()
    data class ConnectionRefused(val hostname: String, val port: Int) : ReachabilityResult()
    data class Unreachable(
        val hostname: String,
        val port: Int,
        val reason: String?
    ) : ReachabilityResult()
}
