package com.dlx.sshterm.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore for app settings.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GRACE_PERIOD = intPreferencesKey("grace_period_minutes")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val TMUX_AUTO_ATTACH = booleanPreferencesKey("tmux_auto_attach")
        val TMUX_SESSION_NAME = stringPreferencesKey("tmux_session_name")
        val TERMINAL_FONT_SIZE = floatPreferencesKey("terminal_font_size")
        val TERMINAL_COLUMNS = intPreferencesKey("terminal_columns")
        val TERMINAL_ROWS = intPreferencesKey("terminal_rows")
        val TERMINAL_SCROLLBACK_SIZE = intPreferencesKey("terminal_scrollback_size")
        val BATTERY_OPTIMIZATION_DISABLED = booleanPreferencesKey("battery_optimization_disabled")
        val TAILSCALE_HOST_TYPE_DETECTION = booleanPreferencesKey("tailscale_host_type_detection")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BIOMETRIC_AUTH_ENABLED = booleanPreferencesKey("biometric_auth_enabled")
        val TERMINAL_EDGE_TO_EDGE = booleanPreferencesKey("terminal_edge_to_edge")
    }

    /**
     * Observes all app settings.
     */
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            gracePeriodMinutes = preferences[Keys.GRACE_PERIOD] ?: AppSettings.DEFAULT_GRACE_PERIOD,
            autoReconnect = preferences[Keys.AUTO_RECONNECT] ?: AppSettings.DEFAULT_AUTO_RECONNECT,
            tmuxAutoAttach = preferences[Keys.TMUX_AUTO_ATTACH] ?: AppSettings.DEFAULT_TMUX_AUTO_ATTACH,
            tmuxSessionName = preferences[Keys.TMUX_SESSION_NAME],
            terminalFontSize = preferences[Keys.TERMINAL_FONT_SIZE] ?: AppSettings.DEFAULT_FONT_SIZE,
            terminalColumns = preferences[Keys.TERMINAL_COLUMNS] ?: AppSettings.DEFAULT_COLUMNS,
            terminalRows = preferences[Keys.TERMINAL_ROWS] ?: AppSettings.DEFAULT_ROWS,
            terminalScrollbackSize = preferences[Keys.TERMINAL_SCROLLBACK_SIZE] ?: AppSettings.DEFAULT_SCROLLBACK_SIZE,
            batteryOptimizationDisabled = preferences[Keys.BATTERY_OPTIMIZATION_DISABLED]
                ?: AppSettings.DEFAULT_BATTERY_OPTIMIZATION_DISABLED,
            tailscaleHostTypeDetection = preferences[Keys.TAILSCALE_HOST_TYPE_DETECTION]
                ?: AppSettings.DEFAULT_TAILSCALE_HOST_TYPE_DETECTION,
            keepScreenOn = preferences[Keys.KEEP_SCREEN_ON] ?: AppSettings.DEFAULT_KEEP_SCREEN_ON,
            biometricAuthEnabled = preferences[Keys.BIOMETRIC_AUTH_ENABLED] ?: AppSettings.DEFAULT_BIOMETRIC_AUTH_ENABLED,
            terminalEdgeToEdge = preferences[Keys.TERMINAL_EDGE_TO_EDGE] ?: AppSettings.DEFAULT_TERMINAL_EDGE_TO_EDGE
        )
    }

    /**
     * Sets the grace period duration in minutes.
     */
    suspend fun setGracePeriod(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GRACE_PERIOD] = minutes
        }
    }

    /**
     * Sets whether auto-reconnect is enabled.
     */
    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AUTO_RECONNECT] = enabled
        }
    }

    /**
     * Sets whether tmux auto-attach is enabled.
     */
    suspend fun setTmuxAutoAttach(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TMUX_AUTO_ATTACH] = enabled
        }
    }

    /**
     * Sets the tmux session name.
     */
    suspend fun setTmuxSessionName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name == null) {
                preferences.remove(Keys.TMUX_SESSION_NAME)
            } else {
                preferences[Keys.TMUX_SESSION_NAME] = name
            }
        }
    }

    /**
     * Sets the terminal font size.
     */
    suspend fun setTerminalFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TERMINAL_FONT_SIZE] = size
        }
    }

    /**
     * Sets the terminal columns.
     */
    suspend fun setTerminalColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TERMINAL_COLUMNS] = columns
        }
    }

    /**
     * Sets the terminal rows.
     */
    suspend fun setTerminalRows(rows: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TERMINAL_ROWS] = rows
        }
    }

    /**
     * Sets the terminal scrollback buffer size in lines.
     */
    suspend fun setScrollbackSize(lines: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TERMINAL_SCROLLBACK_SIZE] = lines
        }
    }

    suspend fun setBatteryOptimizationDisabled(disabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BATTERY_OPTIMIZATION_DISABLED] = disabled
        }
    }

    suspend fun setTailscaleHostTypeDetection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TAILSCALE_HOST_TYPE_DETECTION] = enabled
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun setBiometricAuthEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIC_AUTH_ENABLED] = enabled
        }
    }

    suspend fun setTerminalEdgeToEdge(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TERMINAL_EDGE_TO_EDGE] = enabled
        }
    }
}
