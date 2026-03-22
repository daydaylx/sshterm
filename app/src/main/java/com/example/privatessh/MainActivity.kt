package com.example.privatessh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.privatessh.navigation.AppRoutes
import androidx.navigation.compose.rememberNavController
import com.example.privatessh.navigation.AppNavHost
import com.example.privatessh.ui.theme.PrivateSSHTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Private SSH application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingTerminalRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTerminalRoute.value = resolveTerminalRoute(intent)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            PrivateSSHTheme {
                LaunchedEffect(pendingTerminalRoute.value) {
                    pendingTerminalRoute.value?.let { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                        pendingTerminalRoute.value = null
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTerminalRoute.value = resolveTerminalRoute(intent)
    }

    private fun resolveTerminalRoute(intent: Intent?): String? {
        val sessionId = intent?.getStringExtra("session_id")?.takeIf { it.isNotBlank() } ?: return null
        return AppRoutes.terminal(sessionId)
    }
}
