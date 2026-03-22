package com.example.privatessh.domain.usecase.session

import com.example.privatessh.ssh.SshSessionEngine
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Resizes the terminal PTY dimensions.
 */
@ViewModelScoped
class ResizeTerminalUseCase @Inject constructor(
    private val sessionEngine: SshSessionEngine
) {

    /**
     * Resizes the terminal to the specified dimensions.
     */
    suspend operator fun invoke(columns: Int, rows: Int) = withContext(Dispatchers.IO) {
        try {
            sessionEngine.resizeTerminal(columns, rows)
            true
        } catch (e: Exception) {
            Timber.w(e, "Terminal resize failed")
            false
        }
    }

    /**
     * Resets terminal to default size (80x24).
     */
    suspend fun resetToDefault(): Boolean {
        return invoke(80, 24)
    }
}
