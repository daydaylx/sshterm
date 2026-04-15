package com.dlx.sshterm.domain.usecase.session

import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import com.dlx.sshterm.ssh.SshSessionEngine
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Stops the current SSH session.
 */
@ViewModelScoped
class StopSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke() = withContext(dispatchers.io) {
        try {
            sessionEngine.disconnect()
        } catch (e: Exception) {
            Timber.w(e, "Disconnect error (non-critical)")
        }
    }
}
