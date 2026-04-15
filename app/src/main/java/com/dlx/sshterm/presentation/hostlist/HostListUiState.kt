package com.dlx.sshterm.presentation.hostlist

import com.dlx.sshterm.domain.model.HostProfile

enum class HostGroup { RECENT, TAILSCALE, DIRECT, OTHER }

/**
 * UI State for the host list screen.
 */
data class HostListUiState(
    val hosts: List<HostProfile> = emptyList(),
    val groupedHosts: List<Pair<HostGroup, List<HostProfile>>> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleting: Set<String> = emptySet()
) {
    val isEmpty: Boolean
        get() = hosts.isEmpty() && !isLoading

    fun isDeletingHost(hostId: String): Boolean = hostId in isDeleting
}
