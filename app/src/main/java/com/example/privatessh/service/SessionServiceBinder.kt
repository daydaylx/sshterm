package com.example.privatessh.service

import android.os.Binder
import javax.inject.Inject

/**
 * Binder for communication between UI and TerminalSessionService.
 * Allows UI to bind to the service and access session control methods.
 */
class SessionServiceBinder @Inject constructor(
    private val service: TerminalSessionService
) : Binder() {

    /**
     * Returns the associated service instance.
     */
    fun getService(): TerminalSessionService = service
}
