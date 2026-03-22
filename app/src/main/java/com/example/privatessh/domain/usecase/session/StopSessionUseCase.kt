package com.example.privatessh.domain.usecase.session

import com.example.privatessh.ssh.SshSessionEngine
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Stops the current SSH session.
 */
@ViewModelScoped
class StopSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine
) {

    suspend operator fun invoke() = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            sessionEngine.disconnect()
        } catch (e: Exception) {
            Timber.w(e, "Disconnect error (non-critical)")
        }
    }
}
