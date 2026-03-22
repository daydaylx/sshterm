package com.example.privatessh.domain.usecase.settings

import com.example.privatessh.domain.model.SessionPolicy
import com.example.privatessh.domain.model.TerminalMetrics
import com.example.privatessh.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case for observing app settings.
 */
class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Observes both session policy and terminal metrics.
     */
    operator fun invoke(): Flow<Pair<SessionPolicy, TerminalMetrics>> =
        combine(
            settingsRepository.observeSessionPolicy(),
            settingsRepository.observeTerminalMetrics()
        ) { policy, metrics -> policy to metrics }

    /**
     * Observes only session policy.
     */
    fun observeSessionPolicy(): Flow<SessionPolicy> =
        settingsRepository.observeSessionPolicy()

    /**
     * Observes only terminal metrics.
     */
    fun observeTerminalMetrics(): Flow<TerminalMetrics> =
        settingsRepository.observeTerminalMetrics()

    fun observeBatteryOptimizationDisabled(): Flow<Boolean> =
        settingsRepository.observeBatteryOptimizationDisabled()

    fun observeTailscaleHostTypeDetection(): Flow<Boolean> =
        settingsRepository.observeTailscaleHostTypeDetection()

    fun observeKeepScreenOn(): Flow<Boolean> =
        settingsRepository.observeKeepScreenOn()

    fun observeBiometricAuthEnabled(): Flow<Boolean> =
        settingsRepository.observeBiometricAuthEnabled()
}
