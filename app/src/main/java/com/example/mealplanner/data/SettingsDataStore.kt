package com.example.mealplanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mealplanner.model.AccentPalette
import com.example.mealplanner.model.AppThemeMode
import com.example.mealplanner.model.SettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "meal_planner_settings")

class SettingsDataStore(private val context: Context) {

    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { preferences ->
        SettingsState(
            persistDataBetweenLaunches =
                preferences[Keys.PERSIST_DATA_BETWEEN_LAUNCHES] ?: true,
            clearShoppingAfterExport =
                preferences[Keys.CLEAR_SHOPPING_AFTER_EXPORT] ?: false,
            themeMode = preferences[Keys.THEME_MODE]
                ?.let { raw -> runCatching { AppThemeMode.valueOf(raw) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            accentPalette = preferences[Keys.ACCENT_PALETTE]
                ?.let { raw -> runCatching { AccentPalette.valueOf(raw) }.getOrNull() }
                ?: AccentPalette.EMERALD
        )
    }

    suspend fun setPersistDataBetweenLaunches(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PERSIST_DATA_BETWEEN_LAUNCHES] = value
        }
    }

    suspend fun setClearShoppingAfterExport(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CLEAR_SHOPPING_AFTER_EXPORT] = value
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ACCENT_PALETTE] = palette.name
        }
    }

    private object Keys {
        val PERSIST_DATA_BETWEEN_LAUNCHES = booleanPreferencesKey("persist_data_between_launches")
        val CLEAR_SHOPPING_AFTER_EXPORT = booleanPreferencesKey("clear_shopping_after_export")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    }
}
