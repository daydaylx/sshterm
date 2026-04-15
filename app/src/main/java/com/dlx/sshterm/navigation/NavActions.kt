package com.dlx.sshterm.navigation

/**
 * Navigation actions for type-safe navigation.
 */
sealed class NavAction(val route: String) {
    data object HostList : NavAction(AppRoutes.HOST_LIST)
    data object HostCreate : NavAction(AppRoutes.HOST_CREATE)
    data class HostEdit(val hostId: String) : NavAction(AppRoutes.hostEdit(hostId))
    data class Terminal(val hostId: String) : NavAction(AppRoutes.terminal(hostId))
    data object Settings : NavAction(AppRoutes.SETTINGS)
    data object Diagnostics : NavAction(AppRoutes.DIAGNOSTICS)
}
