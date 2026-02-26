package com.example.mealplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.mealplanner.model.AccentPalette
import com.example.mealplanner.model.AppThemeMode
import com.example.mealplanner.model.DensityMode
import com.example.mealplanner.ui.presentation.toUiDensitySpec

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
    densityMode: DensityMode,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = resolveColorScheme(accentPalette = accentPalette, darkTheme = useDarkTheme)
    val fontScale = densityMode.toUiDensitySpec().fontScaleMultiplier

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography(fontScale),
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

private fun scaledTypography(multiplier: Float): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.scaled(multiplier),
        displayMedium = base.displayMedium.scaled(multiplier),
        displaySmall = base.displaySmall.scaled(multiplier),
        headlineLarge = base.headlineLarge.scaled(multiplier),
        headlineMedium = base.headlineMedium.scaled(multiplier),
        headlineSmall = base.headlineSmall.scaled(multiplier),
        titleLarge = base.titleLarge.scaled(multiplier),
        titleMedium = base.titleMedium.scaled(multiplier),
        titleSmall = base.titleSmall.scaled(multiplier),
        bodyLarge = base.bodyLarge.scaled(multiplier),
        bodyMedium = base.bodyMedium.scaled(multiplier),
        bodySmall = base.bodySmall.scaled(multiplier),
        labelLarge = base.labelLarge.scaled(multiplier),
        labelMedium = base.labelMedium.scaled(multiplier),
        labelSmall = base.labelSmall.scaled(multiplier)
    )
}

private fun TextStyle.scaled(multiplier: Float): TextStyle {
    return copy(
        fontSize = fontSize * multiplier,
        lineHeight = lineHeight * multiplier,
        letterSpacing = letterSpacing * multiplier
    )
}
