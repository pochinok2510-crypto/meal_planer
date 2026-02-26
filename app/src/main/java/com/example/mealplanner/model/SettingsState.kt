package com.example.mealplanner.model

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class AccentPalette {
    EMERALD,
    OCEAN,
    SUNSET,
    LAVENDER
}

enum class DensityMode {
    COMPACT,
    NORMAL
}

data class SettingsState(
    val persistDataBetweenLaunches: Boolean = true,
    val clearShoppingAfterExport: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.EMERALD,
    val densityMode: DensityMode = DensityMode.NORMAL
)
