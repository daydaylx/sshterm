package com.dlx.sshterm.presentation.hostlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dlx.sshterm.domain.model.HostProfile
import com.dlx.sshterm.domain.usecase.host.DeleteHostUseCase
import com.dlx.sshterm.domain.usecase.host.GetHostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private val THREE_DAYS_MS = 86_400_000L * 3

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
                        groupedHosts = groupHosts(hosts),
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
            } catch (e: Exception) {
                _effect.value = HostListUiEffect.ShowDeleteError(e.message)
            } finally {
                _uiState.value = _uiState.value.copy(
                    isDeleting = _uiState.value.isDeleting - hostId
                )
            }
        }
    }

    fun clearEffect() {
        _effect.value = null
    }

    private fun groupHosts(hosts: List<HostProfile>): List<Pair<HostGroup, List<HostProfile>>> {
        val cutoff = System.currentTimeMillis() - THREE_DAYS_MS
        val recent = hosts
            .filter { it.lastConnectedAt != null && it.lastConnectedAt > cutoff }
            .sortedByDescending { it.lastConnectedAt }
        val recentIds = recent.map { it.id }.toSet()
        val remaining = hosts.filter { it.id !in recentIds }
        val tailscale = remaining.filter { it.isTailscale() }
        val direct = remaining.filter { !it.isTailscale() }

        return buildList {
            if (recent.isNotEmpty()) add(HostGroup.RECENT to recent)
            if (tailscale.isNotEmpty()) add(HostGroup.TAILSCALE to tailscale)
            if (direct.isNotEmpty()) add(HostGroup.DIRECT to direct)
            if (isEmpty() && hosts.isNotEmpty()) add(HostGroup.OTHER to hosts)
        }
    }
}
