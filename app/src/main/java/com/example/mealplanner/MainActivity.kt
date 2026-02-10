package com.example.mealplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mealplanner.ui.MealPlannerApp
import com.example.mealplanner.ui.theme.MealPlannerTheme
import com.example.mealplanner.viewmodel.MealPlannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MealPlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MealPlannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MealPlannerApp(viewModel = viewModel)
                }
            }
        }
    }
}
