package com.dlx.sshterm.service

import com.dlx.sshterm.service.SessionGraceController.GraceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for managing grace period after app is removed from recents.
 * Keeps SSH session alive for a configurable time before disconnecting.
 */
@Singleton
class SessionGraceController @Inject constructor(
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
    @Volatile
    private var remainingMinutes: Int = 0

    /**
     * Default grace period duration in minutes.
     */
    var gracePeriodMinutes: Int = 10

    /**
     * Starts the grace period countdown.
     */
    fun startGracePeriod(scope: CoroutineScope, onGraceExpired: () -> Unit) {
        graceJob?.cancel()
        remainingMinutes = gracePeriodMinutes
        _state.value = GraceState.Active(remainingMinutes)

        graceJob = scope.launch {
            while (remainingMinutes > 0) {
                delay(60_000) // Wait 1 minute
                remainingMinutes--
                _state.value = GraceState.Active(remainingMinutes)
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
        remainingMinutes = 0
        _state.value = GraceState.Inactive
    }

    /**
     * Extends the grace period by the specified minutes.
     */
    fun extendGracePeriod(additionalMinutes: Int, maxMinutes: Int = Int.MAX_VALUE) {
        if (additionalMinutes <= 0) return

        _state.update { current ->
            if (current is GraceState.Active) {
                remainingMinutes = (current.minutesRemaining + additionalMinutes)
                    .coerceAtMost(maxMinutes)
                GraceState.Active(remainingMinutes)
            } else current
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
        return (_state.value as? GraceState.Active)?.minutesRemaining ?: 0
    }
}
