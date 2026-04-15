package com.dlx.sshterm.domain.model

/**
 * Authentication type for SSH connection.
 */
enum class AuthType {
    /** Password authentication */
    PASSWORD,

    /** Private key authentication */
    PRIVATE_KEY
}
