package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = FlameRedPrimary,
    onPrimary = Color.White,
    primaryContainer = FlameRedContainerLight,
    onPrimaryContainer = OnFlameRedContainerLight,
    secondary = GlobeBlack,
    onSecondary = Color.White,
    secondaryContainer = GlobeContainerLight,
    onSecondaryContainer = OnGlobeContainerLight,
    tertiary = ShieldSilver,
    onTertiary = Color.White,
    tertiaryContainer = ShieldSilverContainerLight,
    onTertiaryContainer = OnShieldSilverContainerLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FlameRedPrimaryDark,
    onPrimary = Color(0xFF560000),
    primaryContainer = FlameRedContainerDark,
    onPrimaryContainer = OnFlameRedContainerDark,
    secondary = Color(0xFFE2E8F0),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = ShieldSilverLight,
    onTertiary = Color(0xFF1E293B),
    tertiaryContainer = Color(0xFF37474F),
    onTertiaryContainer = Color(0xFFECEFF1),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode.value,
    // Keep consistent church branding rather than letting dynamic system wallpaper override logo colors
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
