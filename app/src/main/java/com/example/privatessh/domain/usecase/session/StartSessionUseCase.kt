package com.example.privatessh.domain.usecase.session

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.ssh.SshSessionEngine
import com.example.privatessh.ssh.hostkey.HostKeyDecision
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Starts an SSH session with a host profile.
 */

/**
 * Starts an SSH session with a host profile.
 */
@ViewModelScoped
class StartSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine
) {

    suspend operator fun invoke(
        hostProfile: HostProfile,
        onHostKeyUnknown: (algorithm: String, fingerprint: String) -> HostKeyDecision
    ): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            sessionEngine.connect(
                hostProfile = hostProfile,
                onHostKeyDecision = onHostKeyUnknown
            )
        } catch (e: Exception) {
            false
        }
    }
}
