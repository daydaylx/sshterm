package com.dlx.sshterm.presentation.hostedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlx.sshterm.domain.model.AuthType
import com.dlx.sshterm.domain.model.NetworkTargetType
import com.dlx.sshterm.domain.usecase.host.DeleteHostUseCase
import com.dlx.sshterm.domain.usecase.host.GetHostByIdUseCase
import com.dlx.sshterm.domain.usecase.host.SaveHostUseCase
import com.dlx.sshterm.navigation.AppRoutes
import com.dlx.sshterm.ssh.auth.PrivateKeyAuthStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostEditViewModel @Inject constructor(
    private val getHostByIdUseCase: GetHostByIdUseCase,
    private val saveHostUseCase: SaveHostUseCase,
    private val deleteHostUseCase: DeleteHostUseCase,
    private val validator: HostEditValidator,
    private val privateKeyAuthStrategy: PrivateKeyAuthStrategy,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val hostId: String? = savedStateHandle[AppRoutes.HOST_ID_ARG]
    private val _uiState = MutableStateFlow(HostEditUiState(isNewHost = hostId == null))
    val uiState = _uiState.asStateFlow()

    init {
        hostId?.let(::loadHost)
    }

    fun onFieldChange(field: String, value: String) {
        val errors = _uiState.value.validationErrors.toMutableMap()
        validator.validateField(field, value)?.let { errors[field] = it } ?: errors.remove(field)
        _uiState.value = _uiState.value.copy(validationErrors = errors, generalError = null)
    }

    fun onAuthTypeChange(authType: AuthType) {
        _uiState.value = _uiState.value.copy(
            hostProfile = _uiState.value.hostProfile?.copy(authType = authType)
        )
    }

    fun onTargetTypeChange(targetType: NetworkTargetType) {
        _uiState.value = _uiState.value.copy(
            hostProfile = _uiState.value.hostProfile?.copy(targetType = targetType)
        )
    }

    fun onSave(
        name: String,
        host: String,
        port: String,
        user: String,
        authType: AuthType,
        targetType: NetworkTargetType,
        privateKeyPem: String
    ) {
        viewModelScope.launch {
            val profile = validator.createProfileFromFields(
                name = name,
                host = host,
                port = port,
                user = user,
                authType = authType,
                targetType = targetType,
                existingProfile = _uiState.value.hostProfile
            )
            val errors = validator.validate(profile)
            if (errors.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(validationErrors = errors)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSaving = true, didSave = false)
            try {
                saveHostUseCase(profile)
                val keyStored = if (authType == AuthType.PRIVATE_KEY && privateKeyPem.isNotBlank()) {
                    privateKeyAuthStrategy.storePrivateKey(profile.id, privateKeyPem.trim())
                } else {
                    _uiState.value.hasStoredPrivateKey
                }
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    hostProfile = profile,
                    validationErrors = emptyMap(),
                    didSave = true,
                    hasStoredPrivateKey = keyStored
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    generalError = "Failed to save host: ${e.message}"
                )
            }
        }
    }

    fun onDelete() {
        val currentHostId = _uiState.value.hostProfile?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, didDelete = false)
            try {
                deleteHostUseCase(currentHostId)
                privateKeyAuthStrategy.clearPrivateKey(currentHostId)
                _uiState.value = _uiState.value.copy(isDeleting = false, didDelete = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    generalError = "Failed to delete host: ${e.message}"
                )
            }
        }
    }

    fun clearGeneralError() {
        _uiState.value = _uiState.value.copy(generalError = null)
    }

    fun onClearPrivateKey() {
        val currentHostId = _uiState.value.hostProfile?.id ?: return
        viewModelScope.launch {
            privateKeyAuthStrategy.clearPrivateKey(currentHostId)
            _uiState.value = _uiState.value.copy(hasStoredPrivateKey = false)
        }
    }

    private fun loadHost(id: String) {
        viewModelScope.launch {
            val host = getHostByIdUseCase(id)
            if (host != null) {
                val hasKey = privateKeyAuthStrategy.hasPrivateKey(host.id)
                _uiState.value = HostEditUiState(
                    isNewHost = false,
                    hostProfile = host,
                    hasStoredPrivateKey = hasKey
                )
            } else {
                _uiState.value = HostEditUiState(isNewHost = false, generalError = "Host not found")
            }
        }
    }
}
