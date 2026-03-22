package com.example.privatessh.ssh

/**
 * SSH session states.
 */
enum class SshSessionState {
    /** No active connection */
    DISCONNECTED,

    /** Establishing connection */
    CONNECTING,

    /** Authenticating */
    AUTHENTICATING,

    /** Connected and authenticated */
    CONNECTED,

    /** Error occurred */
    ERROR,

    /** Disconnecting */
    DISCONNECTING
}
