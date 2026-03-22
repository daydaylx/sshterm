package com.example.privatessh.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver for handling notification actions.
 * Handles Reconnect, Disconnect, and Return actions from the notification.
 */
@AndroidEntryPoint
class SessionActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionRegistry: SessionRegistry

    @Inject
    lateinit var graceController: SessionGraceController

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            SessionNotificationFactory.ACTION_RECONNECT -> {
                // TODO: Trigger reconnect for the session
                val sessionId = intent.getStringExtra("session_id")
            }
            SessionNotificationFactory.ACTION_DISCONNECT -> {
                // TODO: Disconnect the session
                val sessionId = intent.getStringExtra("session_id")
                graceController.stopGracePeriod()
            }
        }
    }
}
