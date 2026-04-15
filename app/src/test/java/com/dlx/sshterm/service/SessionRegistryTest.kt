package com.dlx.sshterm.service

import com.dlx.sshterm.domain.model.AuthType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRegistryTest {

    private lateinit var registry: SessionRegistry

    @Before
    fun setUp() {
        registry = SessionRegistry()
    }

    // ── Fixture ──────────────────────────────────────────────────────────────

    private fun session(id: String = "s1") = ActiveSession(
        sessionId = id,
        hostId = "h1",
        hostName = "example.com",
        authType = AuthType.PASSWORD,
        reconnectAllowed = true,
        passwordCached = true,
        privateKeyAvailable = false
    )

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun initialState_isIdle_withNoSession() {
        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.IDLE, state.lifecycleState)
        assertNull(state.activeSession)
        assertEquals(0, state.sessionCount)
        assertEquals(0, state.graceMinutesRemaining)
        assertNull(state.statusMessage)
        assertFalse(registry.hasActiveSession())
        assertNull(registry.getActiveSession())
        assertNull(registry.getActiveSessionId())
    }

    // ── registerSession ───────────────────────────────────────────────────────

    @Test
    fun registerSession_setsConnectedStateAndSession() {
        val session = session()
        registry.registerSession(session)

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.CONNECTED, state.lifecycleState)
        assertEquals(session, state.activeSession)
        assertEquals(1, state.sessionCount)
        assertEquals(0, state.graceMinutesRemaining)
        assertTrue(registry.hasActiveSession())
        assertEquals(session, registry.getActiveSession())
        assertEquals("s1", registry.getActiveSessionId())
    }

    @Test
    fun registerSession_setsStatusMessageFromShellStatus() {
        registry.registerSession(session().copy(shellStatus = "tmux active"))
        assertEquals("tmux active", registry.runtimeState.value.statusMessage)
    }

    @Test
    fun registerSession_resetsGraceMinutes() {
        registry.registerSession(session())
        registry.markGracePeriod(5)

        registry.registerSession(session().copy(sessionId = "s2"))

        assertEquals(0, registry.runtimeState.value.graceMinutesRemaining)
    }

    // ── updateSession ─────────────────────────────────────────────────────────

    @Test
    fun updateSession_preservesLifecycleState() {
        val session = session()
        registry.registerSession(session)
        registry.markGracePeriod(3)

        val updated = session.copy(shellStatus = "tmux 3:bash")
        registry.updateSession(updated)

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.GRACE, state.lifecycleState)
        assertEquals(updated, state.activeSession)
        assertEquals("tmux 3:bash", state.statusMessage)
        assertEquals(1, state.sessionCount)
    }

    // ── markGracePeriod ───────────────────────────────────────────────────────

    @Test
    fun markGracePeriod_setsGraceStateAndMinutes() {
        registry.registerSession(session())
        registry.markGracePeriod(15)

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.GRACE, state.lifecycleState)
        assertEquals(15, state.graceMinutesRemaining)
        assertNotNull(state.activeSession)
        assertTrue(state.canReconnect)
    }

    @Test
    fun markGracePeriod_hasNullStatusMessage() {
        registry.registerSession(session())
        registry.markGracePeriod(5)

        assertNull(registry.runtimeState.value.statusMessage)
    }

    @Test
    fun markGracePeriod_withoutSession_setsZeroSessionCount() {
        registry.markGracePeriod(5)
        assertEquals(0, registry.runtimeState.value.sessionCount)
    }

    // ── markReconnecting ──────────────────────────────────────────────────────

    @Test
    fun markReconnecting_setsReconnectingStateAndClearsGrace() {
        registry.registerSession(session())
        registry.markGracePeriod(10)
        registry.markReconnecting("Connection lost")

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.RECONNECTING, state.lifecycleState)
        assertEquals(0, state.graceMinutesRemaining)
        assertEquals("Connection lost", state.statusMessage)
    }

    @Test
    fun markReconnecting_acceptsNullReason() {
        registry.registerSession(session())
        registry.markReconnecting(null)

        assertEquals(SessionLifecycleState.RECONNECTING, registry.runtimeState.value.lifecycleState)
        assertNull(registry.runtimeState.value.statusMessage)
    }

    // ── markDisconnecting ─────────────────────────────────────────────────────

    @Test
    fun markDisconnecting_setsDisconnectingState() {
        registry.registerSession(session())
        registry.markDisconnecting()

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.DISCONNECTING, state.lifecycleState)
        assertEquals(0, state.graceMinutesRemaining)
        assertNull(state.statusMessage)
    }

    // ── markFailed ────────────────────────────────────────────────────────────

    @Test
    fun markFailed_setsFailedStateWithReason() {
        registry.registerSession(session())
        registry.markFailed("Auth rejected")

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.FAILED, state.lifecycleState)
        assertEquals("Auth rejected", state.statusMessage)
        assertEquals(0, state.graceMinutesRemaining)
    }

    @Test
    fun markFailed_acceptsNullReason() {
        registry.registerSession(session())
        registry.markFailed(null)

        assertEquals(SessionLifecycleState.FAILED, registry.runtimeState.value.lifecycleState)
        assertNull(registry.runtimeState.value.statusMessage)
    }

    // ── markConnected ─────────────────────────────────────────────────────────

    @Test
    fun markConnected_setsConnectedStateAndClearsGrace() {
        registry.registerSession(session())
        registry.markGracePeriod(5)
        registry.markConnected("tmux active")

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.CONNECTED, state.lifecycleState)
        assertEquals(0, state.graceMinutesRemaining)
        assertEquals("tmux active", state.statusMessage)
    }

    @Test
    fun markConnected_withNoMessage_hasNullStatusMessage() {
        registry.registerSession(session())
        registry.markFailed("Error")
        registry.markConnected()

        assertNull(registry.runtimeState.value.statusMessage)
    }

    // ── clearAll ──────────────────────────────────────────────────────────────

    @Test
    fun clearAll_resetsToInitialState() {
        registry.registerSession(session())
        registry.markGracePeriod(10)
        registry.clearAll()

        val state = registry.runtimeState.value
        assertEquals(SessionLifecycleState.IDLE, state.lifecycleState)
        assertNull(state.activeSession)
        assertEquals(0, state.sessionCount)
        assertEquals(0, state.graceMinutesRemaining)
        assertNull(state.statusMessage)
        assertFalse(registry.hasActiveSession())
    }

    // ── canReconnect ──────────────────────────────────────────────────────────

    @Test
    fun canReconnect_isFalseWhenNoSession() {
        assertFalse(registry.runtimeState.value.canReconnect)
    }

    @Test
    fun canReconnect_isFalseWhenSessionDoesNotAllowReconnect() {
        registry.registerSession(session().copy(reconnectAllowed = false))
        assertFalse(registry.runtimeState.value.canReconnect)
    }

    @Test
    fun canReconnect_isTrueWhenSessionAllowsReconnect() {
        registry.registerSession(session().copy(reconnectAllowed = true))
        assertTrue(registry.runtimeState.value.canReconnect)
    }

    // ── sessionCount consistency ──────────────────────────────────────────────

    @Test
    fun sessionCount_matchesSessionPresence_throughLifecycle() {
        assertEquals(0, registry.runtimeState.value.sessionCount)

        registry.registerSession(session())
        assertEquals(1, registry.runtimeState.value.sessionCount)

        registry.markGracePeriod(5)
        assertEquals(1, registry.runtimeState.value.sessionCount)

        registry.markReconnecting("retry")
        assertEquals(1, registry.runtimeState.value.sessionCount)

        registry.clearAll()
        assertEquals(0, registry.runtimeState.value.sessionCount)
    }
}
