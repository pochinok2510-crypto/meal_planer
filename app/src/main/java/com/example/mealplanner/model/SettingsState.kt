package com.example.mealplanner.model

data class SettingsState(
    val persistDataBetweenLaunches: Boolean = true,
    val clearShoppingAfterExport: Boolean = false
)
