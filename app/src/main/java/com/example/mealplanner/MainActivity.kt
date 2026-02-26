package com.example.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.mealplanner.ui.MealPlannerApp
import com.example.mealplanner.ui.theme.MealPlannerTheme
import com.example.mealplanner.viewmodel.AppViewModel
import com.example.mealplanner.viewmodel.MealPlannerViewModel

class MainActivity : ComponentActivity() {

    private val mealPlannerViewModel: MealPlannerViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by appViewModel.settings.collectAsState()

            MealPlannerTheme(
                themeMode = settings.themeMode,
                accentPalette = settings.accentPalette
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MealPlannerApp(
                        mealPlannerViewModel = mealPlannerViewModel,
                        appViewModel = appViewModel
                    )
                }
            }
        }
    }
}
