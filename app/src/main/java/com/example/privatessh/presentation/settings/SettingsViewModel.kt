package com.example.privatessh.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.privatessh.domain.usecase.settings.GetSettingsUseCase
import com.example.privatessh.domain.usecase.settings.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState.default())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { (policy, metrics) ->
                _uiState.value = _uiState.value.copy(
                    gracePeriodMinutes = policy.gracePeriodMinutes,
                    autoReconnect = policy.autoReconnect,
                    tmuxAutoAttach = policy.tmuxAutoAttach,
                    tmuxSessionName = policy.tmuxSessionName.orEmpty(),
                    terminalFontSize = metrics.fontSize,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            getSettingsUseCase.observeBatteryOptimizationDisabled().collect { disabled ->
                _uiState.value = _uiState.value.copy(batteryOptimizationDisabled = disabled)
            }
        }
        viewModelScope.launch {
            getSettingsUseCase.observeTailscaleHostTypeDetection().collect { enabled ->
                _uiState.value = _uiState.value.copy(tailscaleHostTypeDetection = enabled)
            }
        }
        viewModelScope.launch {
            getSettingsUseCase.observeKeepScreenOn().collect { enabled ->
                _uiState.value = _uiState.value.copy(keepScreenOn = enabled)
            }
        }
    }

    fun setGracePeriod(minutes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(gracePeriodMinutes = minutes)
            updateSettingsUseCase.setGracePeriod(minutes)
        }
    }

    fun toggleAutoReconnect() {
        viewModelScope.launch {
            val value = !_uiState.value.autoReconnect
            _uiState.value = _uiState.value.copy(autoReconnect = value)
            updateSettingsUseCase.setAutoReconnect(value)
        }
    }

    fun toggleTmuxAutoAttach() {
        viewModelScope.launch {
            val value = !_uiState.value.tmuxAutoAttach
            _uiState.value = _uiState.value.copy(tmuxAutoAttach = value)
            updateSettingsUseCase.setTmuxAutoAttach(value)
        }
    }

    fun setTmuxSessionName(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(tmuxSessionName = name)
            updateSettingsUseCase.setTmuxSessionName(name.trim().ifEmpty { null })
        }
    }

    fun setTerminalFontSize(size: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(terminalFontSize = size)
            updateSettingsUseCase.setTerminalFontSize(size)
        }
    }

    fun toggleBatteryOptimization() {
        viewModelScope.launch {
            val value = !_uiState.value.batteryOptimizationDisabled
            _uiState.value = _uiState.value.copy(batteryOptimizationDisabled = value)
            updateSettingsUseCase.setBatteryOptimizationDisabled(value)
        }
    }

    fun toggleTailscaleDetection() {
        viewModelScope.launch {
            val value = !_uiState.value.tailscaleHostTypeDetection
            _uiState.value = _uiState.value.copy(tailscaleHostTypeDetection = value)
            updateSettingsUseCase.setTailscaleHostTypeDetection(value)
        }
    }

    fun toggleKeepScreenOn() {
        viewModelScope.launch {
            val value = !_uiState.value.keepScreenOn
            _uiState.value = _uiState.value.copy(keepScreenOn = value)
            updateSettingsUseCase.setKeepScreenOn(value)
        }
    }
}
