package com.pisces.xcodebn.easycapture.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.pisces.xcodebn.easycapture.ui.theme.*

private val ZergColorScheme = darkColorScheme(
    primary = ZergPurple40,
    secondary = ZergMagenta40,
    tertiary = ZergViolet40,
    background = ZergBackground,
    surface = ZergSurface,
    onPrimary = ZergDark,
    onSecondary = ZergDark,
    onBackground = ZergPurple80,
    onSurface = ZergPurple80
)

private val LightColorScheme = lightColorScheme(
    primary = ZergPurple40,
    secondary = ZergMagenta40,
    tertiary = ZergViolet40
)

@Composable
fun EasyCaptureTheme(
    darkTheme: Boolean = true, // Always use Zerg dark theme
    // Dynamic color disabled for consistent Zerg theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ZergColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}