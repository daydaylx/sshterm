package com.example.privatessh.data.repository

import com.example.privatessh.data.local.datastore.AppSettings
import com.example.privatessh.data.local.datastore.SettingsDataStore
import com.example.privatessh.domain.model.SessionPolicy
import com.example.privatessh.domain.model.TerminalMetrics
import com.example.privatessh.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Repository implementation for app settings.
 */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSessionPolicy(): Flow<SessionPolicy> =
        settingsDataStore.settings.map { it.toSessionPolicy() }

    override fun observeTerminalMetrics(): Flow<TerminalMetrics> =
        settingsDataStore.settings.map { it.toTerminalMetrics() }

    override fun observeBatteryOptimizationDisabled(): Flow<Boolean> =
        settingsDataStore.settings.map { it.batteryOptimizationDisabled }

    override fun observeTailscaleHostTypeDetection(): Flow<Boolean> =
        settingsDataStore.settings.map { it.tailscaleHostTypeDetection }

    override fun observeKeepScreenOn(): Flow<Boolean> =
        settingsDataStore.settings.map { it.keepScreenOn }

    override fun observeBiometricAuthEnabled(): Flow<Boolean> =
        settingsDataStore.settings.map { it.biometricAuthEnabled }

    override suspend fun setGracePeriod(minutes: Int) {
        settingsDataStore.setGracePeriod(minutes)
    }

    override suspend fun setAutoReconnect(enabled: Boolean) {
        settingsDataStore.setAutoReconnect(enabled)
    }

    override suspend fun setTmuxAutoAttach(enabled: Boolean) {
        settingsDataStore.setTmuxAutoAttach(enabled)
    }

    override suspend fun setTmuxSessionName(name: String?) {
        settingsDataStore.setTmuxSessionName(name)
    }

    override suspend fun setTerminalFontSize(size: Float) {
        settingsDataStore.setTerminalFontSize(size)
    }

    override suspend fun setScrollbackSize(lines: Int) {
        settingsDataStore.setScrollbackSize(lines)
    }

    override suspend fun setBatteryOptimizationDisabled(disabled: Boolean) {
        settingsDataStore.setBatteryOptimizationDisabled(disabled)
    }

    override suspend fun setTailscaleHostTypeDetection(enabled: Boolean) {
        settingsDataStore.setTailscaleHostTypeDetection(enabled)
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        settingsDataStore.setKeepScreenOn(enabled)
    }

    override suspend fun setBiometricAuthEnabled(enabled: Boolean) {
        settingsDataStore.setBiometricAuthEnabled(enabled)
    }

    override suspend fun getSessionPolicy(): SessionPolicy =
        settingsDataStore.settings.first().toSessionPolicy()

    override suspend fun getTerminalMetrics(): TerminalMetrics =
        settingsDataStore.settings.first().toTerminalMetrics()
}
