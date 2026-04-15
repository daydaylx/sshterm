package com.dlx.sshterm.diagnostics

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class SessionDiagnosticsStore @Inject constructor() {

    companion object {
        const val MAX_EVENTS = 200
        private const val MAX_DETAIL_LENGTH = 16_000
    }

    private val nextId = AtomicLong(0)
    private val _events = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val events: StateFlow<List<DiagnosticEvent>> = _events.asStateFlow()

    fun info(
        category: DiagnosticCategory,
        title: String,
        detail: String? = null,
        sessionId: String? = null,
        hostId: String? = null,
        hostName: String? = null
    ) {
        append(
            level = DiagnosticLevel.INFO,
            category = category,
            title = title,
            detail = detail,
            sessionId = sessionId,
            hostId = hostId,
            hostName = hostName
        )
    }

    fun warn(
        category: DiagnosticCategory,
        title: String,
        detail: String? = null,
        throwable: Throwable? = null,
        sessionId: String? = null,
        hostId: String? = null,
        hostName: String? = null
    ) {
        append(
            level = DiagnosticLevel.WARN,
            category = category,
            title = title,
            detail = buildDetail(detail, throwable),
            sessionId = sessionId,
            hostId = hostId,
            hostName = hostName
        )
    }

    fun error(
        category: DiagnosticCategory,
        title: String,
        detail: String? = null,
        throwable: Throwable? = null,
        sessionId: String? = null,
        hostId: String? = null,
        hostName: String? = null
    ) {
        append(
            level = DiagnosticLevel.ERROR,
            category = category,
            title = title,
            detail = buildDetail(detail, throwable),
            sessionId = sessionId,
            hostId = hostId,
            hostName = hostName
        )
    }

    fun clearSession(sessionId: String) {
        _events.update { events ->
            events.filterNot { event ->
                event.sessionId == sessionId || event.hostId == sessionId
            }
        }
    }

    fun clearAll() {
        _events.value = emptyList()
    }

    private fun append(
        level: DiagnosticLevel,
        category: DiagnosticCategory,
        title: String,
        detail: String?,
        sessionId: String?,
        hostId: String?,
        hostName: String?
    ) {
        val event = DiagnosticEvent(
            id = nextId.incrementAndGet(),
            level = level,
            category = category,
            title = title,
            detail = detail?.trim()?.take(MAX_DETAIL_LENGTH),
            sessionId = sessionId,
            hostId = hostId,
            hostName = hostName
        )

        _events.update { events ->
            (events + event).takeLast(MAX_EVENTS)
        }
    }

    private fun buildDetail(detail: String?, throwable: Throwable?): String? {
        val normalizedDetail = detail?.trim().orEmpty()
        val throwableDetail = throwable?.stackTraceToString()?.trim().orEmpty()

        return listOf(normalizedDetail, throwableDetail)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n\n")
            .ifBlank { null }
    }
}
