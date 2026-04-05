package com.example.privatessh.presentation.diagnostics

import com.example.privatessh.diagnostics.DiagnosticCategory
import com.example.privatessh.diagnostics.SessionDiagnosticsStore
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.service.ActiveSession
import com.example.privatessh.service.SessionLifecycleState
import com.example.privatessh.service.SessionRegistry
import com.example.privatessh.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun uiState_filtersEventsToActiveSession() = runTest {
        val registry = SessionRegistry()
        val store = SessionDiagnosticsStore()
        val viewModel = DiagnosticsViewModel(store, registry)

        registry.registerSession(activeSession())
        store.info(
            category = DiagnosticCategory.CONNECTION,
            title = "visible",
            sessionId = "session-1",
            hostId = "host-1"
        )
        store.warn(
            category = DiagnosticCategory.CONNECTION,
            title = "hidden",
            sessionId = "session-2",
            hostId = "host-2"
        )
        runCurrent()

        assertEquals(listOf("visible"), viewModel.uiState.value.events.map { it.title })
        assertEquals("user@example.com", viewModel.uiState.value.hostName)
    }

    @Test
    fun uiState_marksLastSessionWhenRegistryIsIdleButEventsRemain() = runTest {
        val registry = SessionRegistry()
        val store = SessionDiagnosticsStore()
        val viewModel = DiagnosticsViewModel(store, registry)

        registry.registerSession(activeSession())
        store.error(
            category = DiagnosticCategory.AUTH,
            title = "failed",
            sessionId = "session-1",
            hostId = "host-1"
        )
        runCurrent()

        registry.clearAll()
        runCurrent()

        assertTrue(viewModel.uiState.value.isShowingLastSession)
        assertEquals(SessionLifecycleState.IDLE, viewModel.uiState.value.lifecycleState)
        assertEquals(1, viewModel.uiState.value.events.size)
    }

    @Test
    fun clearVisible_removesCurrentSessionEvents() = runTest {
        val registry = SessionRegistry()
        val store = SessionDiagnosticsStore()
        val viewModel = DiagnosticsViewModel(store, registry)

        registry.registerSession(activeSession())
        store.info(
            category = DiagnosticCategory.SERVICE,
            title = "service-event",
            sessionId = "session-1",
            hostId = "host-1"
        )
        runCurrent()

        viewModel.clearVisible()
        runCurrent()

        assertFalse(viewModel.uiState.value.hasEvents)
    }

    private fun activeSession() = ActiveSession(
        sessionId = "session-1",
        hostId = "host-1",
        hostName = "user@example.com",
        authType = AuthType.PASSWORD,
        reconnectAllowed = true,
        passwordCached = true,
        privateKeyAvailable = false
    )
}
