package com.example.privatessh.ssh.reconnect

import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Exponential backoff policy for reconnection attempts.
 */
class ReconnectBackoffPolicy @Inject constructor(

) {

    private val maxAttempts = 5
    private val baseDelayMs = 1000L
    private val maxDelayMs = 60000L

    /**
     * Calculates the delay for the given attempt number.
     * Uses exponential backoff with jitter.
     */
    fun getDelay(attempt: Int): Long {
        if (attempt >= maxAttempts) return -1

        val exponentialDelay = (baseDelayMs * (2.0.pow(attempt.toDouble()))).toLong()
        val delay = minOf(exponentialDelay, maxDelayMs)

        // Add jitter (±25%)
        val jitter = (delay * 0.25).toLong()
        return delay - jitter + (Math.random() * jitter * 2).toLong()
    }

    /**
     * Suspends for the calculated delay period.
     * Returns false if max attempts reached.
     */
    suspend fun waitForBackoff(attempt: Int): Boolean {
        val delay = getDelay(attempt)
        if (delay < 0) return false

        delay(delay)
        return true
    }

    /**
     * Returns true if more attempts are allowed.
     */
    fun canRetry(attempt: Int): Boolean {
        return attempt < maxAttempts
    }

    /**
     * Resets the backoff state.
     */
    fun reset() {
        // No state to reset in this implementation
    }

    private fun Double.pow(exponent: Double): Double {
        return Math.pow(this, exponent)
    }
}
