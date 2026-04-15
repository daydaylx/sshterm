package com.dlx.sshterm.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionGraceControllerTest {

    @Test
    fun extendGracePeriod_updatesRemainingMinutes_usedByCountdown() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 2
        }

        controller.startGracePeriod(this) {}
        runCurrent()
        assertEquals(2, controller.getRemainingMinutes())

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, controller.getRemainingMinutes())

        controller.extendGracePeriod(additionalMinutes = 10, maxMinutes = 30)
        assertEquals(11, controller.getRemainingMinutes())

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(10, controller.getRemainingMinutes())
        assertTrue(controller.isActive())
    }

    @Test
    fun gracePeriod_expires_afterRemainingMinutesReachZero() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 1
        }
        var expired = false

        controller.startGracePeriod(this) {
            expired = true
        }
        runCurrent()

        advanceTimeBy(60_000)
        runCurrent()

        assertTrue(expired)
        assertFalse(controller.isActive())
        assertEquals(SessionGraceController.GraceState.Expired, controller.state.value)
    }

    @Test
    fun stopGracePeriod_cancelsPendingExpiryCallback() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 1
        }

        controller.startGracePeriod(this) {
            error("Grace expiry should not be invoked after stopGracePeriod")
        }
        runCurrent()

        controller.stopGracePeriod()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(SessionGraceController.GraceState.Inactive, controller.state.value)
        assertFalse(controller.isActive())
    }

    @Test
    fun extendGracePeriod_hasNoEffectWhenInactive() {
        val controller = SessionGraceController()

        controller.extendGracePeriod(additionalMinutes = 10)

        assertFalse(controller.isActive())
        assertEquals(0, controller.getRemainingMinutes())
    }

    @Test
    fun extendGracePeriod_respectsMaxMinutesConstraint() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 5
        }

        controller.startGracePeriod(this) {}
        runCurrent()

        controller.extendGracePeriod(additionalMinutes = 200, maxMinutes = 120)

        assertEquals(120, controller.getRemainingMinutes())
    }

    @Test
    fun extendGracePeriod_withZeroMinutes_isNoop() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 3
        }

        controller.startGracePeriod(this) {}
        runCurrent()
        val before = controller.getRemainingMinutes()

        controller.extendGracePeriod(additionalMinutes = 0)

        assertEquals(before, controller.getRemainingMinutes())
    }

    @Test
    fun getRemainingMinutes_returnsZeroWhenInactive() {
        val controller = SessionGraceController()
        assertEquals(0, controller.getRemainingMinutes())
    }

    @Test
    fun getRemainingMinutes_returnsZeroAfterExpiry() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 1
        }

        controller.startGracePeriod(this) {}
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(0, controller.getRemainingMinutes())
    }

    @Test
    fun startGracePeriod_cancelsExistingJob() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 10
        }
        var firstExpired = false

        controller.startGracePeriod(this) { firstExpired = true }
        runCurrent()

        // Restart with a short period — first callback must not fire
        controller.startGracePeriod(this) {}
        runCurrent()
        advanceTimeBy(600_000)
        runCurrent()

        assertFalse(firstExpired)
    }

    @Test
    fun stateFlow_transitionsCorrectlyThroughLifecycle() = runTest {
        val controller = SessionGraceController().apply {
            gracePeriodMinutes = 2
        }

        assertEquals(SessionGraceController.GraceState.Inactive, controller.state.value)

        controller.startGracePeriod(this) {}
        runCurrent()
        assertTrue(controller.state.value is SessionGraceController.GraceState.Active)
        assertEquals(2, (controller.state.value as SessionGraceController.GraceState.Active).minutesRemaining)

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, (controller.state.value as SessionGraceController.GraceState.Active).minutesRemaining)

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(SessionGraceController.GraceState.Expired, controller.state.value)

        controller.stopGracePeriod()
        assertEquals(SessionGraceController.GraceState.Inactive, controller.state.value)
    }
}
