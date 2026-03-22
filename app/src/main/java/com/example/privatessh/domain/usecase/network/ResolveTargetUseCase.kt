package com.example.privatessh.domain.usecase.network

import com.example.privatessh.core.network.NetworkType
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Resolves the network type for a given hostname.
 * Determines if a target is Tailscale, VPN, or direct network.
 */
@ViewModelScoped
class ResolveTargetUseCase @Inject constructor(

) {

    /**
     * Determines the network type for a given hostname.
     */
    operator fun invoke(hostname: String): NetworkType {
        // First, check if hostname suggests Tailscale
        val detectedType = NetworkType.detectFromHostname(hostname)
        if (detectedType != NetworkType.UNKNOWN) {
            return detectedType
        }

        // Check if it's a local network hostname
        if (isLocalNetwork(hostname)) {
            return NetworkType.WIFI
        }

        // Check for common VPN patterns
        if (isVpnHostname(hostname)) {
            return NetworkType.VPN
        }

        // Default to unknown
        return NetworkType.UNKNOWN
    }

    /**
     * Checks if a hostname appears to be on the local network.
     */
    private fun isLocalNetwork(hostname: String): Boolean {
        return hostname.endsWith(".local") ||
               hostname.endsWith(".lan") ||
               hostname.endsWith(".home") ||
               hostname.startsWith("192.168.") ||
               hostname.startsWith("10.") ||
               hostname.startsWith("172.16.")
    }

    /**
     * Checks if a hostname appears to be a VPN target.
     */
    private fun isVpnHostname(hostname: String): Boolean {
        return hostname.endsWith(".vpn") ||
               hostname.contains("-vpn-") ||
               hostname.contains("vpn.")
    }

    /**
     * Returns true if a host profile uses Tailscale.
     */
    fun isTailscaleTarget(targetType: com.example.privatessh.domain.model.NetworkTargetType): Boolean {
        return targetType == com.example.privatessh.domain.model.NetworkTargetType.TAILSCALE
    }
}
