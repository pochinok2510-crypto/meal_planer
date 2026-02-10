package com.example.mealplanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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
                preferences[Keys.CLEAR_SHOPPING_AFTER_EXPORT] ?: false
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

    private object Keys {
        val PERSIST_DATA_BETWEEN_LAUNCHES = booleanPreferencesKey("persist_data_between_launches")
        val CLEAR_SHOPPING_AFTER_EXPORT = booleanPreferencesKey("clear_shopping_after_export")
    }
}
