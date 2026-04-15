package com.dlx.sshterm.domain.usecase.session

import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import com.dlx.sshterm.ssh.SshSessionEngine
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Sends input to the SSH terminal session.
 */
@ViewModelScoped
class SendTerminalInputUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine,
    private val dispatchers: DispatcherProvider
) {

    /**
     * Sends raw byte data to the SSH session.
     */
    suspend operator fun invoke(data: ByteArray) = withContext(dispatchers.io) {
        try {
            sessionEngine.sendInput(data)
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to send terminal input")
            false
        }
    }

    /**
     * Sends a string to the SSH session.
     */
    suspend fun sendString(text: String): Boolean {
        return invoke(text.toByteArray())
    }

    /**
     * Sends a special key sequence (like Ctrl+C).
     */
    suspend fun sendSpecialKey(sequence: ByteArray): Boolean {
        return invoke(sequence)
    }
}
