package com.example.mealplanner.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mealplanner.ui.navigation.Screen
import com.example.mealplanner.ui.screens.AddMealScreen
import com.example.mealplanner.ui.screens.MenuScreen
import com.example.mealplanner.ui.screens.SettingsScreen
import com.example.mealplanner.ui.screens.ShoppingListScreen
import com.example.mealplanner.viewmodel.MealPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerApp(viewModel: MealPlannerViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val meals by viewModel.meals.collectAsState()
    val selectedMeals by viewModel.selectedMealNames.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val destinations = listOf(Screen.Menu, Screen.AddMeal, Screen.ShoppingList, Screen.Settings)

    Scaffold(
        topBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            TopAppBar(title = {
                Text(
                    when (currentRoute) {
                        Screen.Menu.route -> "Меню"
                        Screen.AddMeal.route -> "Добавить блюдо"
                        Screen.ShoppingList.route -> "Список покупок"
                        Screen.Settings.route -> "Настройки"
                        else -> "Meal Planner"
                    }
                )
            })
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { screen ->
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(iconFor(screen)) },
                        label = { Text(labelFor(screen)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Menu.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(Screen.Menu.route) {
                MenuScreen(
                    meals = meals,
                    selectedMeals = selectedMeals,
                    onMealSelectionToggle = viewModel::toggleMealSelection,
                    onRemoveMeal = viewModel::removeMeal,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToShopping = { navController.navigate(Screen.ShoppingList.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.AddMeal.route) {
                AddMealScreen(
                    onBack = { navController.popBackStack() },
                    onSaveMeal = { name, ingredients ->
                        viewModel.addMeal(name, ingredients)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.ShoppingList.route) {
                ShoppingListScreen(
                    ingredients = viewModel.getAggregatedShoppingList(),
                    onBack = { navController.popBackStack() },
                    onClear = viewModel::clearShoppingSelection,
                    onExport = {
                        val file = viewModel.exportShoppingListToPdf()
                        val message = if (file == null) {
                            "Нечего экспортировать"
                        } else {
                            "PDF сохранён: ${file.name}"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onPersistDataToggle = viewModel::updatePersistDataBetweenLaunches,
                    onClearAfterExportToggle = viewModel::updateClearShoppingAfterExport
                )
            }
        }
    }
}

private fun iconFor(screen: Screen): String = when (screen) {
    Screen.Menu -> "📋"
    Screen.AddMeal -> "➕"
    Screen.ShoppingList -> "🛒"
    Screen.Settings -> "⚙️"
}

private fun labelFor(screen: Screen): String = when (screen) {
    Screen.Menu -> "Меню"
    Screen.AddMeal -> "Добавить"
    Screen.ShoppingList -> "Покупки"
    Screen.Settings -> "Настройки"
}
