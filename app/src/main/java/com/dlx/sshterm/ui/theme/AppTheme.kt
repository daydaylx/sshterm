package com.dlx.sshterm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppTheme {
    val backgroundBrush: Brush
        @Composable
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF08131A),
                Color(0xFF0B1720),
                Color(0xFF04080C)
            )
        )

    val heroBrush: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                Color(0xFF123142),
                Color(0xFF10222E),
                Color(0xFF0A141B)
            )
        )

    val terminalBackdropBrush: Brush
        @Composable
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF061016),
                TerminalBackground
            )
        )

    val panelColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)

    val panelStrongColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f)

    val panelBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)

    val glowColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    val success: Color
        @Composable
        get() = StatusConnected

    val warning: Color
        @Composable
        get() = StatusConnecting

    val danger: Color
        @Composable
        get() = StatusError

    val info: Color
        @Composable
        get() = TailscaleAccent
}
