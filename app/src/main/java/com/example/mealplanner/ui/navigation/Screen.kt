package com.example.mealplanner.ui.navigation

sealed class Screen(val route: String) {
    data object Menu : Screen("menu")
    data object AddMeal : Screen("add_meal")
    data object ShoppingList : Screen("shopping_list")
    data object Settings : Screen("settings")
}
