package com.example.privatessh.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionDiagnosticsStoreTest {

    @Test
    fun append_keepsOnlyLatest200Events() {
        val store = SessionDiagnosticsStore()

        repeat(205) { index ->
            store.info(
                category = DiagnosticCategory.CONNECTION,
                title = "event-$index",
                sessionId = "session-1",
                hostId = "host-1"
            )
        }

        val events = store.events.value
        assertEquals(SessionDiagnosticsStore.MAX_EVENTS, events.size)
        assertEquals("event-5", events.first().title)
        assertEquals("event-204", events.last().title)
    }

    @Test
    fun clearSession_removesMatchingSessionAndHostEntries() {
        val store = SessionDiagnosticsStore()

        store.info(
            category = DiagnosticCategory.CONNECTION,
            title = "keep",
            sessionId = "session-keep",
            hostId = "host-keep"
        )
        store.error(
            category = DiagnosticCategory.AUTH,
            title = "drop-session",
            sessionId = "session-drop",
            hostId = "host-drop"
        )
        store.warn(
            category = DiagnosticCategory.UI,
            title = "drop-host",
            hostId = "session-drop"
        )

        store.clearSession("session-drop")

        val remainingTitles = store.events.value.map { it.title }
        assertEquals(listOf("keep"), remainingTitles)
    }

    @Test
    fun error_includesThrowableStackTrace() {
        val store = SessionDiagnosticsStore()

        store.error(
            category = DiagnosticCategory.CONNECTION,
            title = "connect failed",
            throwable = IllegalStateException("boom")
        )

        val detail = store.events.value.single().detail.orEmpty()
        assertTrue(detail.contains("IllegalStateException"))
        assertTrue(detail.contains("boom"))
    }
}
