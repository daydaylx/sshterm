package com.example.privatessh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.privatessh.navigation.AppNavHost
import com.example.privatessh.ui.theme.PrivateSSHTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Private SSH application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PrivateSSHTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    AppNavHost(
                        navController = rememberNavController(),
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
