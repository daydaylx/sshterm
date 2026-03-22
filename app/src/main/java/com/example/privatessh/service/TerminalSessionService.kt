package com.example.privatessh.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.privatessh.domain.model.AuthType
import com.example.privatessh.domain.repository.SettingsRepository
import com.example.privatessh.ssh.SshSessionEngine
import com.example.privatessh.ssh.SshSessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that keeps the single active SSH session process alive.
 */
@AndroidEntryPoint
class TerminalSessionService : Service() {

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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoReconnectEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        observeSettings()
        observeGracePeriod()
        observeEngineState()
    }

    override fun onBind(intent: Intent?): IBinder = SessionServiceBinder(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SessionNotificationFactory.ACTION_RECONNECT -> handleReconnect(manual = true)
            SessionNotificationFactory.ACTION_DISCONNECT -> handleDisconnect()
            SessionNotificationFactory.ACTION_START_SESSION, null -> {
                val sessionId = intent?.getStringExtra("session_id")
                val hostName = intent?.getStringExtra("host_name")
                if (!sessionId.isNullOrBlank() && !hostName.isNullOrBlank()) {
                    startSession(sessionId, hostName)
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
        super.onDestroy()
        graceController.stopGracePeriod()
        notificationFactory.cancelNotification()
        sessionRegistry.clearAll()
        serviceScope.cancel()
    }

    fun stopSession() {
        graceController.stopGracePeriod()
        notificationFactory.cancelNotification()
        sessionRegistry.clearAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSession(sessionId: String, hostName: String) {
        graceController.stopGracePeriod()
        val activeSession = buildActiveSession(sessionId, hostName)
        sessionRegistry.registerSession(activeSession)
        startForeground(
            SessionNotificationFactory.NOTIFICATION_ID,
            notificationFactory.createSessionNotification(activeSession.hostName, activeSession.sessionId)
        )
    }

    private fun handleReconnect(manual: Boolean) {
        graceController.stopGracePeriod()
        val activeSession = sessionRegistry.getActiveSession() ?: return
        val capability = sessionEngine.canReconnect()
        val refreshedSession = activeSession.copy(
            reconnectAllowed = capability.isAllowed,
            passwordCached = capability.passwordCached,
            privateKeyAvailable = capability.privateKeyAvailable
        )
        sessionRegistry.updateSession(refreshedSession)

        if (!capability.isAllowed) {
            val reason = capability.reason ?: "Reconnect is not available."
            sessionRegistry.markFailed(reason)
            notificationFactory.updateNotification(
                notificationFactory.createDisconnectedNotification(
                    hostName = refreshedSession.hostName,
                    sessionId = refreshedSession.sessionId,
                    reason = reason,
                    canReconnect = false
                )
            )
            return
        }

        val statusMessage = if (manual) {
            "Reconnect requested"
        } else {
            sessionEngine.error.value ?: "Connection lost, reconnecting"
        }
        sessionRegistry.markReconnecting(statusMessage)
        startForeground(
            SessionNotificationFactory.NOTIFICATION_ID,
            notificationFactory.createReconnectingNotification(
                hostName = refreshedSession.hostName,
                sessionId = refreshedSession.sessionId,
                reason = statusMessage
            )
        )

        serviceScope.launch {
            val reconnected = sessionEngine.reconnectLast()
            if (reconnected) {
                val connectedSession = buildActiveSession(
                    sessionId = refreshedSession.sessionId,
                    fallbackHostName = refreshedSession.hostName
                )
                sessionRegistry.registerSession(connectedSession)
                notificationFactory.updateNotification(
                    notificationFactory.createSessionNotification(
                        hostName = connectedSession.hostName,
                        sessionId = connectedSession.sessionId
                    )
                )
            } else {
                val failedCapability = sessionEngine.canReconnect()
                val failedSession = refreshedSession.copy(
                    reconnectAllowed = failedCapability.isAllowed,
                    passwordCached = failedCapability.passwordCached,
                    privateKeyAvailable = failedCapability.privateKeyAvailable
                )
                val reason = sessionEngine.error.value ?: "Reconnect failed"
                sessionRegistry.updateSession(failedSession)
                sessionRegistry.markFailed(reason)
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
        serviceScope.launch {
            sessionEngine.disconnect()
            stopSession()
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.observeSessionPolicy().collectLatest { policy ->
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
                            notificationFactory.updateNotification(
                                notificationFactory.createSessionNotification(
                                    hostName = activeSession.hostName,
                                    sessionId = activeSession.sessionId
                                )
                            )
                        }
                    }

                    is SessionGraceController.GraceState.Active -> {
                        sessionRegistry.markGracePeriod(state.minutesRemaining)
                        notificationFactory.updateNotification(
                            notificationFactory.createGracePeriodNotification(
                                hostName = activeSession.hostName,
                                sessionId = activeSession.sessionId,
                                minutesRemaining = state.minutesRemaining
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
                                sessionId = connectedSession.sessionId
                            )
                        )
                    }

                    SshSessionState.ERROR -> {
                        if (runtimeState.lifecycleState in setOf(
                                SessionLifecycleState.DISCONNECTING,
                                SessionLifecycleState.GRACE,
                                SessionLifecycleState.RECONNECTING,
                                SessionLifecycleState.FAILED
                            )
                        ) {
                            return@collectLatest
                        }
                        val reason = sessionEngine.error.value ?: "SSH session failed"
                        if (autoReconnectEnabled) {
                            handleReconnect(manual = false)
                        } else {
                            sessionRegistry.markFailed(reason)
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
                        if (runtimeState.lifecycleState == SessionLifecycleState.DISCONNECTING) {
                            stopSession()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun buildActiveSession(sessionId: String, fallbackHostName: String): ActiveSession {
        val host = sessionEngine.currentHost.value
        val capability = sessionEngine.canReconnect()
        return ActiveSession(
            sessionId = sessionId,
            hostId = host?.id ?: sessionId,
            hostName = host?.getDisplayName().orEmpty().ifBlank { fallbackHostName },
            authType = host?.authType ?: AuthType.PASSWORD,
            reconnectAllowed = capability.isAllowed,
            passwordCached = capability.passwordCached,
            privateKeyAvailable = capability.privateKeyAvailable
        )
    }
}
