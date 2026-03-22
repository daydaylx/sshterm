package com.example.privatessh.presentation.hostlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.privatessh.domain.model.HostProfile
import com.example.privatessh.domain.usecase.host.DeleteHostUseCase
import com.example.privatessh.domain.usecase.host.GetHostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the host list screen.
 */
@HiltViewModel
class HostListViewModel @Inject constructor(
    private val getHostsUseCase: GetHostsUseCase,
    private val deleteHostUseCase: DeleteHostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HostListUiState())
    val uiState: StateFlow<HostListUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<HostListUiEffect?>(null)
    val effect: StateFlow<HostListUiEffect?> = _effect.asStateFlow()

    init {
        loadHosts()
    }

    private fun loadHosts() {
        viewModelScope.launch {
            getHostsUseCase()
                .catch { error ->
                    _uiState.value = HostListUiState(
                        isLoading = false,
                        error = error.message ?: "Unknown error loading hosts"
                    )
                }
                .collect { hosts ->
                    _uiState.value = HostListUiState(
                        hosts = hosts,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    /**
     * User clicked add host button.
     */
    fun onAddHostClick() {
        _effect.value = HostListUiEffect.NavigateToAddHost
    }

    /**
     * User clicked edit host.
     */
    fun onEditHostClick(hostId: String) {
        _effect.value = HostListUiEffect.NavigateToEditHost(hostId)
    }

    /**
     * User clicked connect on a host.
     */
    fun onConnectClick(hostId: String) {
        _effect.value = HostListUiEffect.NavigateToTerminal(hostId)
    }

    /**
     * User clicked delete on a host (show confirmation).
     */
    fun onDeleteClick(host: HostProfile) {
        _effect.value = HostListUiEffect.ShowDeleteDialog(
            hostId = host.id,
            hostName = host.getDisplayName()
        )
    }

    /**
     * User confirmed delete.
     */
    fun onConfirmDelete(hostId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeleting = _uiState.value.isDeleting + hostId
            )

            try {
                deleteHostUseCase(hostId)
                _effect.value = HostListUiEffect.ShowToast("Host deleted")
            } catch (e: Exception) {
                _effect.value = HostListUiEffect.ShowToast(
                    "Failed to delete: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isDeleting = _uiState.value.isDeleting - hostId
                )
            }
        }
    }

    /**
     * Clears the current effect.
     */
    fun clearEffect() {
        _effect.value = null
    }
}
