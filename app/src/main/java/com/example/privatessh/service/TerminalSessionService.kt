package com.example.privatessh.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.privatessh.ssh.SshSessionEngine

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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _currentHost = MutableStateFlow<String?>(null)
    val currentHost = _currentHost.asStateFlow()

    override fun onBind(intent: Intent?): IBinder = SessionServiceBinder(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SessionNotificationFactory.ACTION_RECONNECT -> handleReconnect()
            SessionNotificationFactory.ACTION_DISCONNECT -> handleDisconnect()
            else -> {
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
        if (sessionRegistry.hasActiveSession()) {
            graceController.startGracePeriod(serviceScope) {
                handleDisconnect()
            }
            updateGraceNotification()
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
        sessionRegistry.getActiveSessionId()?.let { sessionRegistry.unregisterSession(it) }
        _currentHost.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSession(sessionId: String, hostName: String) {
        graceController.stopGracePeriod()
        _currentHost.value = hostName
        sessionRegistry.registerSession(sessionId)
        startForeground(
            SessionNotificationFactory.NOTIFICATION_ID,
            notificationFactory.createSessionNotification(hostName, sessionId)
        )
    }

    private fun handleReconnect() {
        graceController.stopGracePeriod()
        val sessionId = sessionRegistry.getActiveSessionId() ?: return
        val hostName = _currentHost.value ?: sessionEngine.currentHost.value?.getDisplayName() ?: return

        serviceScope.launch {
            val reconnected = sessionEngine.reconnectLast()
            val notification = if (reconnected) {
                notificationFactory.createSessionNotification(hostName, sessionId)
            } else {
                notificationFactory.createDisconnectedNotification(hostName, sessionId)
            }
            notificationFactory.updateNotification(notification)
        }
    }

    private fun handleDisconnect() {
        serviceScope.launch {
            sessionEngine.disconnect()
            stopSession()
        }
    }

    private fun updateGraceNotification() {
        serviceScope.launch {
            graceController.state.collect { state ->
                val hostName = _currentHost.value ?: return@collect
                if (state is SessionGraceController.GraceState.Active) {
                    notificationFactory.updateNotification(
                        notificationFactory.createGracePeriodNotification(hostName, state.minutesRemaining)
                    )
                }
            }
        }
    }
}
