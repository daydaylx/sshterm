package com.example.privatessh.presentation.hostlist

/**
 * One-time UI effects for the host list screen.
 */
sealed class HostListUiEffect {
    /**
     * Navigate to host edit screen for creating a new host.
     */
    data object NavigateToAddHost : HostListUiEffect()

    /**
     * Navigate to host edit screen for editing an existing host.
     */
    data class NavigateToEditHost(val hostId: String) : HostListUiEffect()

    /**
     * Navigate to terminal screen for a host.
     */
    data class NavigateToTerminal(val hostId: String) : HostListUiEffect()

    /**
     * Show delete confirmation dialog.
     */
    data class ShowDeleteDialog(val hostId: String, val hostName: String) : HostListUiEffect()

    /**
     * Show toast message.
     */
    data class ShowToast(val message: String) : HostListUiEffect()
}
