package com.dlx.sshterm.ssh

/**
 * SSH session states.
 */
enum class SshSessionState {
    /** No active connection */
    DISCONNECTED,

    /** Establishing connection */
    CONNECTING,

    /** Re-establishing a previous connection */
    RECONNECTING,

    /** Authenticating */
    AUTHENTICATING,

    /** Connected and authenticated */
    CONNECTED,

    /** Error occurred */
    ERROR,

    /** Disconnecting */
    DISCONNECTING
}
