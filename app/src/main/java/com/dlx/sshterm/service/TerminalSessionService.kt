package com.dlx.sshterm.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.dlx.sshterm.R
import com.dlx.sshterm.core.constants.Defaults
import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import com.dlx.sshterm.diagnostics.DiagnosticCategory
import com.dlx.sshterm.diagnostics.SessionDiagnosticsStore
import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.SessionPolicy
import com.dlx.sshterm.domain.repository.SettingsRepository
import com.dlx.sshterm.ssh.SessionShellMode
import com.dlx.sshterm.ssh.SshSessionEngine
import com.dlx.sshterm.ssh.SshSessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the single active SSH session process alive.
 */
@AndroidEntryPoint
class TerminalSessionService : Service() {

    companion object {
        private const val GRACE_EXTENSION_MINUTES = 10
    }

    @Inject
    lateinit var sessionRegistry: SessionRegistry

    @Inject
    lateinit var notificationFactory: SessionNotificationFactory

    @Inject
    lateinit var graceController: SessionGraceController

    @Inject
    lateinit var sessionEngine: SshSessionEngine

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var diagnosticsStore: SessionDiagnosticsStore

    @Inject
    lateinit var dispatchers: DispatcherProvider

    private val serviceScope by lazy { CoroutineScope(SupervisorJob() + dispatchers.mainImmediate) }
    private var autoReconnectEnabled: Boolean = true
    private var currentSessionPolicy: SessionPolicy = SessionPolicy()
    private var reconnectAttempts: Int = 0

    override fun onCreate() {
        super.onCreate()
        diagnosticsStore.info(
            category = DiagnosticCategory.SERVICE,
            title = "Terminal-Service erstellt",
            detail = "Der Foreground-Service für SSH-Sitzungen wurde gestartet."
        )
        observeSettings()
        observeGracePeriod()
        observeEngineState()
        observeShellStatus()
    }

    override fun onBind(intent: Intent?): IBinder = SessionServiceBinder(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SessionNotificationFactory.ACTION_RECONNECT -> serviceScope.launch { handleReconnect(manual = true) }
            SessionNotificationFactory.ACTION_DISCONNECT -> handleDisconnect()
            SessionNotificationFactory.ACTION_EXTEND_GRACE -> extendGracePeriod()
            SessionNotificationFactory.ACTION_START_SESSION, null -> {
                val sessionId = intent?.getStringExtra("session_id")
                val hostName = intent?.getStringExtra("host_name")
                if (!sessionId.isNullOrBlank() && !hostName.isNullOrBlank()) {
                    serviceScope.launch { startSession(sessionId, hostName) }
                }
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!sessionRegistry.hasActiveSession() || graceController.isActive()) {
            return
        }

        graceController.startGracePeriod(serviceScope) {
            handleDisconnect()
        }
    }

    override fun onDestroy() {
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = "Terminal-Service beendet",
            session = sessionRegistry.getActiveSession(),
            detail = "Der Foreground-Service wurde gestoppt."
        )
        super.onDestroy()
        graceController.stopGracePeriod()
        notificationFactory.cancelNotification()
        sessionRegistry.clearAll()
        serviceScope.cancel()
    }

    fun stopSession() {
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = "Sitzung im Foreground-Service beendet",
            session = sessionRegistry.getActiveSession(),
            detail = "Notifications und Servicezustand werden zurückgesetzt."
        )
        graceController.stopGracePeriod()
        notificationFactory.cancelNotification()
        sessionRegistry.clearAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun startSession(sessionId: String, hostName: String) {
        reconnectAttempts = 0
        graceController.stopGracePeriod()
        val activeSession = buildActiveSession(sessionId, hostName)
        sessionRegistry.registerSession(activeSession)
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = "Sitzung beim Foreground-Service registriert",
            session = activeSession,
            detail = "Session-ID: ${activeSession.sessionId}"
        )
        startForeground(
            SessionNotificationFactory.NOTIFICATION_ID,
            notificationFactory.createSessionNotification(
                hostName = activeSession.hostName,
                sessionId = activeSession.sessionId,
                sessionDetail = activeSession.shellStatus
            )
        )
    }

    private suspend fun handleReconnect(manual: Boolean) {
        graceController.stopGracePeriod()
        val activeSession = sessionRegistry.getActiveSession() ?: return
        val capability = sessionEngine.canReconnect()
        val refreshedSession = activeSession.copy(
            reconnectAllowed = capability.isAllowed,
            passwordCached = capability.passwordCached,
            privateKeyAvailable = capability.privateKeyAvailable
        )
        sessionRegistry.updateSession(refreshedSession)
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = if (manual) "Manueller Reconnect angefordert" else "Automatischer Reconnect angefordert",
            session = refreshedSession,
            detail = capability.reason ?: "Reconnect-Voraussetzungen erfüllt."
        )

        if (!capability.isAllowed) {
            val reason = when {
                capability.requiresPasswordPrompt -> getString(R.string.ssh_password_unavailable)
                !capability.privateKeyAvailable && refreshedSession.authType == com.dlx.sshterm.domain.model.AuthType.PRIVATE_KEY ->
                    getString(R.string.ssh_private_key_unavailable)
                else -> capability.reason ?: getString(R.string.service_reconnect_unavailable)
            }
            sessionRegistry.markFailed(reason)
            logError(
                category = DiagnosticCategory.SERVICE,
                title = "Reconnect nicht verfügbar",
                session = refreshedSession,
                detail = reason
            )
            // When a password is needed, the user must open the app to re-enter it.
            // Show the notification as "can reconnect" so the action opens the app.
            val canReconnectViaApp = capability.requiresPasswordPrompt
            notificationFactory.updateNotification(
                notificationFactory.createDisconnectedNotification(
                    hostName = refreshedSession.hostName,
                    sessionId = refreshedSession.sessionId,
                    reason = reason,
                    canReconnect = canReconnectViaApp
                )
            )
            return
        }

        val statusMessage = if (manual) {
            getString(R.string.service_reconnect_requested)
        } else {
            sessionEngine.error.value ?: getString(R.string.service_connection_lost_reconnecting)
        }
        sessionRegistry.markReconnecting(statusMessage)
        logWarning(
            category = DiagnosticCategory.SERVICE,
            title = "Reconnect läuft",
            session = refreshedSession,
            detail = statusMessage
        )
        startForeground(
            SessionNotificationFactory.NOTIFICATION_ID,
            notificationFactory.createReconnectingNotification(
                hostName = refreshedSession.hostName,
                sessionId = refreshedSession.sessionId,
                reason = statusMessage
            )
        )

        serviceScope.launch {
            val reconnected = sessionEngine.reconnectLast(currentSessionPolicy)
            if (reconnected) {
                reconnectAttempts = 0
                val connectedSession = buildActiveSession(
                    sessionId = refreshedSession.sessionId,
                    fallbackHostName = refreshedSession.hostName
                )
                sessionRegistry.registerSession(connectedSession)
                logInfo(
                    category = DiagnosticCategory.SERVICE,
                    title = "Reconnect erfolgreich",
                    session = connectedSession,
                    detail = "Die Sitzung wurde nach Unterbrechung wiederhergestellt."
                )
                notificationFactory.updateNotification(
                    notificationFactory.createSessionNotification(
                        hostName = connectedSession.hostName,
                        sessionId = connectedSession.sessionId,
                        sessionDetail = connectedSession.shellStatus
                    )
                )
            } else {
                val failedCapability = sessionEngine.canReconnect()
                val failedSession = refreshedSession.copy(
                    reconnectAllowed = failedCapability.isAllowed,
                    passwordCached = failedCapability.passwordCached,
                    privateKeyAvailable = failedCapability.privateKeyAvailable
                )
                val reason = sessionEngine.error.value ?: getString(R.string.service_reconnect_failed)
                sessionRegistry.updateSession(failedSession)
                sessionRegistry.markFailed(reason)
                logError(
                    category = DiagnosticCategory.SERVICE,
                    title = "Reconnect fehlgeschlagen",
                    session = failedSession,
                    detail = reason
                )
                notificationFactory.updateNotification(
                    notificationFactory.createDisconnectedNotification(
                        hostName = failedSession.hostName,
                        sessionId = failedSession.sessionId,
                        reason = reason,
                        canReconnect = failedSession.reconnectAllowed
                    )
                )
            }
        }
    }

    private fun handleDisconnect() {
        sessionRegistry.markDisconnecting()
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = "Disconnect über Service ausgelöst",
            session = sessionRegistry.getActiveSession(),
            detail = "Die Sitzung wird kontrolliert beendet."
        )
        serviceScope.launch {
            sessionEngine.disconnect()
            stopSession()
        }
    }

    private fun extendGracePeriod() {
        val activeSession = sessionRegistry.getActiveSession() ?: return
        if (!graceController.isActive()) return

        graceController.extendGracePeriod(
            additionalMinutes = GRACE_EXTENSION_MINUTES,
            maxMinutes = SessionPolicy.GRACE_PERIOD_MAX
        )
        logInfo(
            category = DiagnosticCategory.SERVICE,
            title = "Nachlaufzeit verlängert",
            session = activeSession,
            detail = "Zusätzliche Minuten: $GRACE_EXTENSION_MINUTES"
        )

        notificationFactory.updateNotification(
            notificationFactory.createGracePeriodNotification(
                hostName = activeSession.hostName,
                sessionId = activeSession.sessionId,
                minutesRemaining = graceController.getRemainingMinutes(),
                sessionDetail = activeSession.shellStatus
            )
        )
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.observeSessionPolicy().collectLatest { policy ->
                currentSessionPolicy = policy
                graceController.gracePeriodMinutes = policy.gracePeriodMinutes
                autoReconnectEnabled = policy.autoReconnect
            }
        }
    }

    private fun observeGracePeriod() {
        serviceScope.launch {
            graceController.state.collectLatest { state ->
                val activeSession = sessionRegistry.getActiveSession() ?: return@collectLatest
                when (state) {
                    SessionGraceController.GraceState.Inactive -> {
                        if (sessionRegistry.runtimeState.value.lifecycleState == SessionLifecycleState.GRACE) {
                            sessionRegistry.markConnected()
                            logInfo(
                                category = DiagnosticCategory.SERVICE,
                                title = "Nachlaufzeit beendet",
                                session = activeSession,
                                detail = "Die Sitzung ist wieder im normalen Verbunden-Zustand."
                            )
                            notificationFactory.updateNotification(
                                notificationFactory.createSessionNotification(
                                    hostName = activeSession.hostName,
                                    sessionId = activeSession.sessionId,
                                    sessionDetail = activeSession.shellStatus
                                )
                            )
                        }
                    }

                    is SessionGraceController.GraceState.Active -> {
                        sessionRegistry.markGracePeriod(state.minutesRemaining)
                        logWarning(
                            category = DiagnosticCategory.SERVICE,
                            title = "Nachlaufzeit aktiv",
                            session = activeSession,
                            detail = "Verbleibende Minuten: ${state.minutesRemaining}"
                        )
                        notificationFactory.updateNotification(
                            notificationFactory.createGracePeriodNotification(
                                hostName = activeSession.hostName,
                                sessionId = activeSession.sessionId,
                                minutesRemaining = state.minutesRemaining,
                                sessionDetail = activeSession.shellStatus
                            )
                        )
                    }

                    SessionGraceController.GraceState.Expired -> Unit
                }
            }
        }
    }

    private fun observeEngineState() {
        serviceScope.launch {
            sessionEngine.state.collectLatest { engineState ->
                val runtimeState = sessionRegistry.runtimeState.value
                val activeSession = runtimeState.activeSession ?: return@collectLatest

                when (engineState) {
                    SshSessionState.CONNECTED -> {
                        val connectedSession = buildActiveSession(
                            sessionId = activeSession.sessionId,
                            fallbackHostName = activeSession.hostName
                        )
                        sessionRegistry.registerSession(connectedSession)
                        notificationFactory.updateNotification(
                            notificationFactory.createSessionNotification(
                                hostName = connectedSession.hostName,
                                sessionId = connectedSession.sessionId,
                                sessionDetail = connectedSession.shellStatus
                            )
                        )
                    }

                    SshSessionState.ERROR -> {
                        val freshState = sessionRegistry.runtimeState.value
                        if (freshState.lifecycleState in setOf(
                                SessionLifecycleState.DISCONNECTING,
                                SessionLifecycleState.GRACE,
                                SessionLifecycleState.RECONNECTING,
                                SessionLifecycleState.FAILED
                            )
                        ) {
                            return@collectLatest
                        }
                        val reason = sessionEngine.error.value ?: getString(R.string.service_session_failed)
                        if (autoReconnectEnabled && reconnectAttempts < Defaults.RECONNECT_MAX_ATTEMPTS) {
                            val backoffMs = (Defaults.RECONNECT_INITIAL_DELAY_MS * (1L shl reconnectAttempts))
                                .coerceAtMost(Defaults.RECONNECT_MAX_DELAY_MS)
                            reconnectAttempts++
                            logWarning(
                                category = DiagnosticCategory.SERVICE,
                                title = "Automatischer Reconnect geplant",
                                session = activeSession,
                                detail = "Versuch $reconnectAttempts mit Backoff ${backoffMs} ms.\nGrund: $reason"
                            )
                            delay(backoffMs)
                            handleReconnect(manual = false)
                        } else {
                            reconnectAttempts = 0
                            sessionRegistry.markFailed(reason)
                            logError(
                                category = DiagnosticCategory.SERVICE,
                                title = "Sitzung endgültig fehlgeschlagen",
                                session = activeSession,
                                detail = reason
                            )
                            notificationFactory.updateNotification(
                                notificationFactory.createDisconnectedNotification(
                                    hostName = activeSession.hostName,
                                    sessionId = activeSession.sessionId,
                                    reason = reason,
                                    canReconnect = activeSession.reconnectAllowed
                                )
                            )
                        }
                    }

                    SshSessionState.DISCONNECTED -> {
                        val freshState = sessionRegistry.runtimeState.value
                        if (freshState.lifecycleState == SessionLifecycleState.DISCONNECTING) {
                            stopSession()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun observeShellStatus() {
        serviceScope.launch {
            sessionEngine.shellStatus.collectLatest { shellStatus ->
                val activeSession = sessionRegistry.getActiveSession() ?: return@collectLatest
                val updatedSession = activeSession.copy(
                    shellMode = shellStatus.mode,
                    shellStatus = shellStatus.message
                )
                sessionRegistry.updateSession(updatedSession)

                when (sessionRegistry.runtimeState.value.lifecycleState) {
                    SessionLifecycleState.CONNECTED -> {
                        notificationFactory.updateNotification(
                            notificationFactory.createSessionNotification(
                                hostName = updatedSession.hostName,
                                sessionId = updatedSession.sessionId,
                                sessionDetail = updatedSession.shellStatus
                            )
                        )
                    }

                    SessionLifecycleState.GRACE -> {
                        notificationFactory.updateNotification(
                            notificationFactory.createGracePeriodNotification(
                                hostName = updatedSession.hostName,
                                sessionId = updatedSession.sessionId,
                                minutesRemaining = sessionRegistry.runtimeState.value.graceMinutesRemaining,
                                sessionDetail = updatedSession.shellStatus
                            )
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun buildActiveSession(sessionId: String, fallbackHostName: String): ActiveSession {
        val host = sessionEngine.currentHost.value
        val capability = sessionEngine.canReconnect()
        val shellStatus = sessionEngine.shellStatus.value
        return ActiveSession(
            sessionId = sessionId,
            hostId = host?.id ?: sessionId,
            hostName = host?.getDisplayName().orEmpty().ifBlank { fallbackHostName },
            authType = host?.authType ?: AuthType.PASSWORD,
            reconnectAllowed = capability.isAllowed,
            passwordCached = capability.passwordCached,
            privateKeyAvailable = capability.privateKeyAvailable,
            shellMode = shellStatus.mode,
            shellStatus = shellStatus.message
        )
    }

    private fun logInfo(
        category: DiagnosticCategory,
        title: String,
        session: ActiveSession? = null,
        detail: String? = null
    ) {
        diagnosticsStore.info(
            category = category,
            title = title,
            detail = detail,
            sessionId = session?.sessionId,
            hostId = session?.hostId,
            hostName = session?.hostName
        )
    }

    private fun logWarning(
        category: DiagnosticCategory,
        title: String,
        session: ActiveSession? = null,
        detail: String? = null
    ) {
        diagnosticsStore.warn(
            category = category,
            title = title,
            detail = detail,
            sessionId = session?.sessionId,
            hostId = session?.hostId,
            hostName = session?.hostName
        )
    }

    private fun logError(
        category: DiagnosticCategory,
        title: String,
        session: ActiveSession? = null,
        detail: String? = null
    ) {
        diagnosticsStore.error(
            category = category,
            title = title,
            detail = detail,
            sessionId = session?.sessionId,
            hostId = session?.hostId,
            hostName = session?.hostName
        )
    }
}
