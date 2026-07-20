package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = DuoGreen,
    onPrimary = White,
    secondary = DuoBlue,
    onSecondary = White,
    tertiary = DuoYellow,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = White,
    onSurface = White,
    error = DuoRed,
    outline = DuoBorder
)

private val LightColorScheme = lightColorScheme(
    primary = DuoGreen,
    onPrimary = White,
    secondary = DuoBlue,
    onSecondary = White,
    tertiary = DuoYellow,
    background = White,
    surface = DuoSurface1,
    onBackground = DuoInk,
    onSurface = DuoInk,
    error = DuoRed,
    outline = DuoBorder
)

@Composable
fun FormFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
