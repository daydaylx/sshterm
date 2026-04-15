package com.dlx.sshterm.domain.usecase.settings

import com.dlx.sshterm.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for updating app settings.
 */
class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Updates the grace period duration.
     */
    suspend fun setGracePeriod(minutes: Int) {
        require(minutes in 1..120) {
            "Grace period must be between 1 and 120 minutes, got $minutes"
        }
        settingsRepository.setGracePeriod(minutes)
    }

    /**
     * Updates the auto-reconnect setting.
     */
    suspend fun setAutoReconnect(enabled: Boolean) {
        settingsRepository.setAutoReconnect(enabled)
    }

    /**
     * Updates the tmux auto-attach setting.
     */
    suspend fun setTmuxAutoAttach(enabled: Boolean) {
        settingsRepository.setTmuxAutoAttach(enabled)
    }

    /**
     * Updates the tmux session name.
     */
    suspend fun setTmuxSessionName(name: String?) {
        settingsRepository.setTmuxSessionName(name)
    }

    /**
     * Updates the terminal font size.
     */
    suspend fun setTerminalFontSize(size: Float) {
        require(size in 8f..32f) {
            "Font size must be between 8 and 32, got $size"
        }
        settingsRepository.setTerminalFontSize(size)
    }

    /**
     * Updates the terminal scrollback buffer size in lines.
     */
    suspend fun setScrollbackSize(lines: Int) {
        require(lines in 100..10_000) {
            "Scrollback size must be between 100 and 10000, got $lines"
        }
        settingsRepository.setScrollbackSize(lines)
    }

    suspend fun setBatteryOptimizationDisabled(disabled: Boolean) {
        settingsRepository.setBatteryOptimizationDisabled(disabled)
    }

    suspend fun setTailscaleHostTypeDetection(enabled: Boolean) {
        settingsRepository.setTailscaleHostTypeDetection(enabled)
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        settingsRepository.setKeepScreenOn(enabled)
    }

    suspend fun setBiometricAuthEnabled(enabled: Boolean) {
        settingsRepository.setBiometricAuthEnabled(enabled)
    }
}
