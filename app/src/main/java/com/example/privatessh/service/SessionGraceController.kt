package com.example.privatessh.service

import com.example.privatessh.service.SessionGraceController.GraceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing grace period after app is removed from recents.
 * Keeps SSH session alive for a configurable time before disconnecting.
 */
@Singleton
class SessionGraceController @Inject constructor(
    private val sessionRegistry: SessionRegistry
) {

    /**
     * State of the grace period.
     */
    sealed class GraceState {
        data object Inactive : GraceState()
        data class Active(val minutesRemaining: Int) : GraceState()
        data object Expired : GraceState()
    }

    private val _state = MutableStateFlow<GraceState>(GraceState.Inactive)
    val state: StateFlow<GraceState> = _state.asStateFlow()

    private var graceJob: Job? = null
    private var graceScope: CoroutineScope? = null

    /**
     * Default grace period duration in minutes.
     */
    var gracePeriodMinutes: Int = 10

    /**
     * Starts the grace period countdown.
     */
    fun startGracePeriod(scope: CoroutineScope, onGraceExpired: () -> Unit) {
        graceScope = scope
        var minutesRemaining = gracePeriodMinutes
        _state.value = GraceState.Active(minutesRemaining)

        graceJob = scope.launch {
            while (minutesRemaining > 0) {
                delay(60_000) // Wait 1 minute
                minutesRemaining--
                if (minutesRemaining > 0) {
                    _state.value = GraceState.Active(minutesRemaining)
                }
            }
            _state.value = GraceState.Expired
            onGraceExpired()
        }
    }

    /**
     * Stops the grace period.
     */
    fun stopGracePeriod() {
        graceJob?.cancel()
        graceJob = null
        graceScope = null
        _state.value = GraceState.Inactive
    }

    /**
     * Extends the grace period by the specified minutes.
     */
    fun extendGracePeriod(additionalMinutes: Int) {
        val currentState = _state.value
        if (currentState is GraceState.Active) {
            _state.value = GraceState.Active(currentState.minutesRemaining + additionalMinutes)
        }
    }

    /**
     * Returns true if grace period is currently active.
     */
    fun isActive(): Boolean {
        return _state.value is GraceState.Active
    }

    /**
     * Returns the remaining minutes in grace period.
     */
    fun getRemainingMinutes(): Int {
        val state = _state.value
        return if (state is GraceState.Active) {
            state.minutesRemaining
        } else {
            0
        }
    }
}
