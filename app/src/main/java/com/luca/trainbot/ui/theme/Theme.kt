package com.luca.trainbot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = BackgroundWhite,
    primaryContainer = SecondaryPurple,
    onPrimaryContainer = BackgroundWhite,
    secondary = AccentBlue,
    onSecondary = BackgroundWhite,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = BackgroundWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = BackgroundWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurpleDark,
    onPrimary = SurfaceDark,
    primaryContainer = PrimaryPurple,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentBlue,
    onSecondary = SurfaceDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceContainerDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Danger,
    onError = BackgroundWhite,
)

@Composable
fun TrainBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color available on Android 12+ — disabled to keep brand palette consistent
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TrainBotTypography,
        content = content,
    )
}
