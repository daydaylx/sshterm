package com.example.privatessh.presentation.hostlist

import com.example.privatessh.domain.model.HostProfile

/**
 * UI State for the host list screen.
 */
data class HostListUiState(
    val hosts: List<HostProfile> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleting: Set<String> = emptySet()
) {
    /**
     * Returns true if the list is empty and not loading.
     */
    val isEmpty: Boolean
        get() = hosts.isEmpty() && !isLoading

    /**
     * Returns true if the host with given id is being deleted.
     */
    fun isDeletingHost(hostId: String): Boolean = hostId in isDeleting
}
