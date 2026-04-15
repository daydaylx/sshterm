package com.dlx.sshterm.domain.model

import com.dlx.sshterm.core.constants.Defaults

/**
 * Session configuration policy.
 */
data class SessionPolicy(
    val gracePeriodMinutes: Int = Defaults.GRACE_PERIOD_MINUTES_DEFAULT,
    val autoReconnect: Boolean = true,
    val tmuxAutoAttach: Boolean = false,
    val tmuxSessionName: String? = null
) {
    companion object {
        const val GRACE_PERIOD_MIN = 1
        const val GRACE_PERIOD_MAX = 120
    }

    /**
     * Validates the grace period is within acceptable range.
     */
    fun isValid(): Boolean {
        return gracePeriodMinutes in GRACE_PERIOD_MIN..GRACE_PERIOD_MAX
    }
}
