package com.example.mealplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mealplanner.model.AccentPalette
import com.example.mealplanner.model.AppThemeMode

private val EmeraldLightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFF66BB6A)
)

private val EmeraldDarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFA5D6A7),
    tertiary = Color(0xFFC8E6C9)
)

private val OceanLightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF1E88E5),
    tertiary = Color(0xFF42A5F5)
)

private val OceanDarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    secondary = Color(0xFF90CAF9),
    tertiary = Color(0xFFBBDEFB)
)

private val SunsetLightColors = lightColorScheme(
    primary = Color(0xFFD84315),
    secondary = Color(0xFFFF7043),
    tertiary = Color(0xFFFF8A65)
)

private val SunsetDarkColors = darkColorScheme(
    primary = Color(0xFFFFAB91),
    secondary = Color(0xFFFFCCBC),
    tertiary = Color(0xFFFFE0B2)
)

private val LavenderLightColors = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    secondary = Color(0xFF8E24AA),
    tertiary = Color(0xFFAB47BC)
)

private val LavenderDarkColors = darkColorScheme(
    primary = Color(0xFFCE93D8),
    secondary = Color(0xFFE1BEE7),
    tertiary = Color(0xFFF3E5F5)
)

@Composable
fun MealPlannerTheme(
    themeMode: AppThemeMode,
    accentPalette: AccentPalette,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = resolveColorScheme(accentPalette = accentPalette, darkTheme = useDarkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private fun resolveColorScheme(accentPalette: AccentPalette, darkTheme: Boolean): ColorScheme {
    return when (accentPalette) {
        AccentPalette.EMERALD -> if (darkTheme) EmeraldDarkColors else EmeraldLightColors
        AccentPalette.OCEAN -> if (darkTheme) OceanDarkColors else OceanLightColors
        AccentPalette.SUNSET -> if (darkTheme) SunsetDarkColors else SunsetLightColors
        AccentPalette.LAVENDER -> if (darkTheme) LavenderDarkColors else LavenderLightColors
    }
}
