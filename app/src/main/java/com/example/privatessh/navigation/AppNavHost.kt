package com.example.privatessh.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.privatessh.ui.hostedit.HostEditScreen
import com.example.privatessh.ui.hostlist.HostListScreen
import com.example.privatessh.ui.diagnostics.DiagnosticsScreen
import com.example.privatessh.ui.settings.SettingsScreen
import com.example.privatessh.ui.terminal.TerminalScreen

/**
 * Main navigation host for the application.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = AppRoutes.HOST_LIST
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        // Host List Screen
        composable(AppRoutes.HOST_LIST) {
            HostListScreen(
                onNavigateToAddHost = {
                    navController.navigate(AppRoutes.HOST_CREATE)
                },
                onNavigateToEditHost = { hostId ->
                    navController.navigate(AppRoutes.hostEdit(hostId))
                },
                onNavigateToTerminal = { hostId ->
                    navController.navigate(AppRoutes.terminal(hostId))
                },
                onNavigateToSettings = {
                    navController.navigate(AppRoutes.SETTINGS)
                }
            )
        }

        // Create Host Screen
        composable(AppRoutes.HOST_CREATE) {
            HostEditScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Edit Host Screen
        composable(
            route = AppRoutes.HOST_EDIT_ROUTE,
            arguments = listOf(
                navArgument(AppRoutes.HOST_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            HostEditScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Terminal Screen (placeholder)
        composable(
            route = AppRoutes.TERMINAL_ROUTE,
            arguments = listOf(
                navArgument(AppRoutes.HOST_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val hostId = backStackEntry.arguments?.getString(AppRoutes.HOST_ID_ARG) ?: return@composable
            TerminalScreen(
                hostId = hostId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = {
                    navController.navigate(AppRoutes.DIAGNOSTICS)
                }
            )
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = {
                    navController.navigate(AppRoutes.DIAGNOSTICS)
                }
            )
        }

        composable(AppRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
