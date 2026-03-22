package com.example.privatessh.domain.usecase.session

import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.repository.SettingsRepository
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
    private val sessionEngine: SshSessionEngine,
    private val settingsRepository: SettingsRepository
) {

    suspend operator fun invoke(
        hostProfile: HostProfile,
        onHostKeyUnknown: suspend (algorithm: String, fingerprint: String) -> HostKeyDecision,
        columns: Int = 80,
        rows: Int = 24
    ): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val sessionPolicy = settingsRepository.getSessionPolicy()
            val terminalMetrics = settingsRepository.getTerminalMetrics()
            sessionEngine.connect(
                hostProfile = hostProfile,
                onHostKeyDecision = onHostKeyUnknown,
                columns = columns,
                rows = rows,
                sessionPolicy = sessionPolicy,
                scrollbackLimit = terminalMetrics.scrollbackSize
            )
        } catch (e: Exception) {
            false
        }
    }
}
