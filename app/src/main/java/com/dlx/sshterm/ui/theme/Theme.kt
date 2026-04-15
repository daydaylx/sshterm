package com.dlx.sshterm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Mint500,
    onPrimary = Ink980,
    primaryContainer = Color(0xFF124233),
    onPrimaryContainer = Mint300,
    secondary = Cyan500,
    onSecondary = Ink980,
    secondaryContainer = Color(0xFF132F3D),
    onSecondaryContainer = Cyan300,
    tertiary = Amber500,
    onTertiary = Ink980,
    tertiaryContainer = Color(0xFF463514),
    onTertiaryContainer = Amber300,
    background = Ink980,
    onBackground = Sand050,
    surface = Ink900,
    onSurface = Sand050,
    surfaceVariant = Ink840,
    onSurfaceVariant = Sand300,
    surfaceContainer = Ink930,
    surfaceContainerHigh = Ink870,
    outline = Ink720,
    outlineVariant = Ink780,
    error = Rose500,
    onError = Ink980,
    errorContainer = Color(0xFF43131A),
    onErrorContainer = Rose300
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F8B64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1F8E0),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF006C93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCAEFFF),
    onSecondaryContainer = Color(0xFF001F2D),
    tertiary = Color(0xFF7A5800),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF261900),
    background = MistyBlue,
    onBackground = Ink950,
    surface = FogSurface,
    onSurface = Ink950,
    surfaceVariant = Color(0xFFDDE7EE),
    onSurfaceVariant = Color(0xFF415462),
    surfaceContainer = Color(0xFFF4F8FB),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outline = Color(0xFF718694),
    outlineVariant = Color(0xFFC0CED8),
    error = Color(0xFFB52233),
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF410007)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun PrivateSSHTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
