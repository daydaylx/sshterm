package com.dlx.sshterm.navigation

/**
 * Navigation routes for the application.
 */
object AppRoutes {
    const val HOST_LIST = "host_list"
    const val HOST_EDIT = "host_edit"
    const val HOST_CREATE = "host_create"
    const val TERMINAL = "terminal"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"

    // Arguments
    const val HOST_ID_ARG = "hostId"

    // Routes with arguments
    const val HOST_EDIT_ROUTE = "$HOST_EDIT/{$HOST_ID_ARG}"
    const val TERMINAL_ROUTE = "$TERMINAL/{$HOST_ID_ARG}"

    fun hostEdit(hostId: String) = "$HOST_EDIT/$hostId"
    fun terminal(hostId: String) = "$TERMINAL/$hostId"
}
