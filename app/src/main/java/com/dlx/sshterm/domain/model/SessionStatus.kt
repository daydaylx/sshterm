package com.dlx.sshterm.domain.model

/**
 * Current status of an SSH session.
 */
enum class SessionStatus {
    /** Not connected */
    DISCONNECTED,

    /** Establishing connection */
    CONNECTING,

    /** Authenticating */
    AUTHENTICATING,

    /** Connected and ready */
    CONNECTED,

    /** Error occurred */
    ERROR,

    /** Session in grace period after app was swiped away */
    GRACE_PERIOD
}
