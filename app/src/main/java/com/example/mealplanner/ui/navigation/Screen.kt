package com.example.mealplanner.ui.navigation

sealed class Screen(
    val route: String,
    val title: String,
    val label: String,
    val icon: String
) {
    data object Menu : Screen(
        route = "menu",
        title = "Меню",
        label = "Меню",
        icon = "📋"
    )

    data object AddMeal : Screen(
        route = "add_meal",
        title = "Добавить блюдо",
        label = "Добавить",
        icon = "➕"
    )

    data object WeeklyPlanner : Screen(
        route = "weekly_planner",
        title = "План недели",
        label = "Неделя",
        icon = "📅"
    )

    data object ShoppingList : Screen(
        route = "shopping_list",
        title = "Список покупок",
        label = "Покупки",
        icon = "🛒"
    )

    data object Settings : Screen(
        route = "settings",
        title = "Настройки",
        label = "Настройки",
        icon = "⚙️"
    )

    companion object {
        val bottomNavigationItems = listOf(Menu, AddMeal, WeeklyPlanner, ShoppingList, Settings)
    }
}
