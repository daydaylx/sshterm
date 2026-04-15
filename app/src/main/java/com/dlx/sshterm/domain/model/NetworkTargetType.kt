package com.dlx.sshterm.domain.model

/**
 * Network target type - helps identify Tailscale hosts.
 */
enum class NetworkTargetType {
    /** Direct connection via IP or hostname */
    DIRECT,

    /** Connection via Tailscale network */
    TAILSCALE
}
