package com.dlx.sshterm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dlx.sshterm.MainActivity
import com.dlx.sshterm.R
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

        const val ACTION_START_SESSION = "com.dlx.sshterm.action.START_SESSION"
        const val ACTION_RECONNECT = "com.dlx.sshterm.action.RECONNECT"
        const val ACTION_DISCONNECT = "com.dlx.sshterm.action.DISCONNECT"
        const val ACTION_EXTEND_GRACE = "com.dlx.sshterm.action.EXTEND_GRACE"

        private const val REQ_OPEN_APP = 1010
        private const val REQ_RECONNECT = 1011
        private const val REQ_DISCONNECT = 1012
        private const val REQ_EXTEND = 1013
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun createSessionNotification(
        hostName: String,
        sessionId: String,
        sessionDetail: String? = null
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_session_active, hostName))
            .setContentText(sessionDetail?.let { "$hostName · $it" } ?: hostName)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.notification_action_return),
                createOpenAppIntent(sessionId)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_disconnect),
                createServiceAction(ACTION_DISCONNECT, sessionId)
            )
            .build()

    fun createReconnectingNotification(
        hostName: String,
        sessionId: String,
        reason: String?
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_reconnecting_title))
            .setContentText(reason ?: hostName)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.notification_action_return),
                createOpenAppIntent(sessionId)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_disconnect),
                createServiceAction(ACTION_DISCONNECT, sessionId)
            )
            .build()

    fun createGracePeriodNotification(
        hostName: String,
        sessionId: String,
        minutesRemaining: Int,
        sessionDetail: String? = null
    ): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_grace_title))
            .setContentText(
                buildString {
                    append(hostName)
                    append(" · ")
                    append(context.getString(R.string.notification_grace_remaining, minutesRemaining))
                    sessionDetail?.let {
                        append(" · ")
                        append(it)
                    }
                }
            )
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.notification_action_return),
                createOpenAppIntent(sessionId)
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                context.getString(R.string.notification_action_extend_grace),
                createServiceAction(ACTION_EXTEND_GRACE, sessionId)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_disconnect),
                createServiceAction(ACTION_DISCONNECT, sessionId)
            )
            .build()

    fun createDisconnectedNotification(
        hostName: String,
        sessionId: String,
        reason: String?,
        canReconnect: Boolean
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_disconnected_title))
            .setContentText(reason ?: hostName)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(createOpenAppIntent(sessionId))
            .addAction(
                android.R.drawable.ic_menu_view,
                context.getString(R.string.notification_action_return),
                createOpenAppIntent(sessionId)
            )
        if (canReconnect) {
            builder.addAction(
                android.R.drawable.ic_menu_rotate,
                context.getString(R.string.notification_action_reconnect),
                createServiceAction(ACTION_RECONNECT, sessionId)
            )
        }
        return builder.build()
    }

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
            REQ_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createServiceAction(action: String, sessionId: String): PendingIntent {
        val intent = Intent(context, TerminalSessionService::class.java).apply {
            this.action = action
            putExtra("session_id", sessionId)
        }
        val requestCode = when (action) {
            ACTION_RECONNECT -> REQ_RECONNECT
            ACTION_DISCONNECT -> REQ_DISCONNECT
            ACTION_EXTEND_GRACE -> REQ_EXTEND
            else -> REQ_OPEN_APP
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
