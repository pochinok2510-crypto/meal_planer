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

data class SettingsState(
    val persistDataBetweenLaunches: Boolean = true,
    val clearShoppingAfterExport: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.EMERALD
)
