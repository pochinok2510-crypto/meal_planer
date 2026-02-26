package com.example.mealplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealplanner.data.SettingsDataStore
import com.example.mealplanner.model.AccentPalette
import com.example.mealplanner.model.AppThemeMode
import com.example.mealplanner.model.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    val settings: StateFlow<SettingsState> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun updateAccentPalette(palette: AccentPalette) {
        viewModelScope.launch {
            settingsDataStore.setAccentPalette(palette)
        }
    }
}
