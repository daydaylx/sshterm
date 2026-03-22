package com.example.privatessh.ssh.reconnect

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

/**
 * Trigger for initiating reconnection attempts.
 */
class ReconnectTrigger @Inject constructor(

) {

    private val _triggerChannel = Channel<ReconnectCause>(Channel.UNLIMITED)

    val triggerFlow: Flow<ReconnectCause>
        get() = _triggerChannel.receiveAsFlow()

    /**
     * Triggers a reconnection with the given cause.
     */
    fun trigger(cause: ReconnectCause) {
        _triggerChannel.trySend(cause)
    }

    /**
     * Triggers reconnection due to connection loss.
     */
    fun triggerConnectionLost() {
        trigger(ReconnectCause.ConnectionLost)
    }

    /**
     * Triggers reconnection due to authentication failure.
     */
    fun triggerAuthFailed() {
        trigger(ReconnectCause.AuthFailed)
    }

    /**
     * Triggers reconnection due to network change.
     */
    fun triggerNetworkChanged() {
        trigger(ReconnectCause.NetworkChanged)
    }

    /**
     * Triggers reconnection due to keepalive timeout.
     */
    fun triggerKeepaliveTimeout() {
        trigger(ReconnectCause.KeepaliveTimeout)
    }
}

/**
 * Reasons for reconnection.
 */
sealed class ReconnectCause {
    data object ConnectionLost : ReconnectCause()
    data object AuthFailed : ReconnectCause()
    data object NetworkChanged : ReconnectCause()
    data object KeepaliveTimeout : ReconnectCause()
    data class UserInitiated(val reason: String) : ReconnectCause()
}
