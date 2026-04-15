package com.dlx.sshterm.domain.usecase.session

import com.dlx.sshterm.core.dispatchers.DispatcherProvider
import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.repository.SettingsRepository
import com.dlx.sshterm.ssh.SshSessionEngine
import com.dlx.sshterm.ssh.hostkey.HostKeyDecision
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Starts an SSH session with a host profile.
 */
@ViewModelScoped
class StartSessionUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        hostProfile: HostProfile,
        onHostKeyUnknown: suspend (algorithm: String, fingerprint: String) -> HostKeyDecision,
        columns: Int = 80,
        rows: Int = 24
    ): Boolean = withContext(dispatchers.io) {
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
