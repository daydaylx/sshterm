package com.dlx.sshterm.core.network

/**
 * Network connection type for SSH targets.
 */
enum class NetworkType {
    WIFI,
    MOBILE,
    TAILSCALE,
    VPN,
    UNKNOWN;

    /**
     * Returns true if this is a Tailscale network.
     */
    fun isTailscale(): Boolean {
        return this == TAILSCALE
    }

    /**
     * Returns true if this is a VPN network.
     */
    fun isVpn(): Boolean {
        return this == VPN || this == TAILSCALE
    }

    /**
     * Returns the display name for this network type.
     */
    fun getDisplayName(): String {
        return when (this) {
            WIFI -> "Wi-Fi"
            MOBILE -> "Mobile Data"
            TAILSCALE -> "Tailscale"
            VPN -> "VPN"
            UNKNOWN -> "Unknown"
        }
    }

    companion object {
        /**
         * Detects if a hostname suggests a Tailscale target.
         * Tailscale hostnames typically end in .ts.net or use specific patterns.
         */
        fun detectFromHostname(hostname: String): NetworkType {
            return when {
                hostname.endsWith(".ts.net") ||
                hostname.endsWith(".tailnet-name.") ||
                hostname.matches(Regex("^[a-f0-9]{8}\\.ts\\.net$")) -> TAILSCALE
                hostname.endsWith(".local") ||
                hostname.endsWith(".lan") -> WIFI
                else -> UNKNOWN
            }
        }
    }
}
