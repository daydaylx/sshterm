package com.example.privatessh.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.privatessh.MainActivity
import com.example.privatessh.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground notification factory for active SSH sessions.
 */
@Singleton
class SessionNotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "ssh_session_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_RECONNECT = "com.example.privatessh.action.RECONNECT"
        const val ACTION_DISCONNECT = "com.example.privatessh.action.DISCONNECT"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun createSessionNotification(hostName: String, sessionId: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_session_active, hostName))
            .setContentText(hostName)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_disconnect),
                createServiceAction(ACTION_DISCONNECT, sessionId)
            )
            .build()

    fun createGracePeriodNotification(hostName: String, minutesRemaining: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SSH session in grace period")
            .setContentText("$hostName · $minutesRemaining min remaining")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(null))
            .build()

    fun createDisconnectedNotification(hostName: String, sessionId: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SSH session disconnected")
            .setContentText(hostName)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Reconnect",
                createServiceAction(ACTION_RECONNECT, sessionId)
            )
            .build()

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun updateNotification(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createOpenAppIntent(sessionId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            sessionId?.let { putExtra("session_id", it) }
        }
        return PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createServiceAction(action: String, sessionId: String): PendingIntent {
        val intent = Intent(context, TerminalSessionService::class.java).apply {
            this.action = action
            putExtra("session_id", sessionId)
        }
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
