package com.example.mealplanner.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
    val groups by viewModel.groups.collectAsState()
    val selectedMeals by viewModel.selectedMealIds.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val dayCount by viewModel.dayCount.collectAsState()

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val saved = viewModel.saveShoppingListPdfToUri(uri)
        Toast.makeText(
            context,
            if (saved) "PDF сохранён" else "Не удалось сохранить PDF",
            Toast.LENGTH_LONG
        ).show()
    }

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
                    groups = groups,
                    selectedMealIds = selectedMeals,
                    onMealSelectionToggle = viewModel::toggleMealSelection,
                    onRemoveMeal = viewModel::removeMeal,
                    onMoveMealToGroup = viewModel::moveMealToGroup,
                    onDuplicateMealToGroup = viewModel::duplicateMealToGroup,
                    onCreateGroup = viewModel::addGroup,
                    onDeleteGroup = viewModel::removeGroup,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToShopping = { navController.navigate(Screen.ShoppingList.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.AddMeal.route) {
                AddMealScreen(
                    groups = groups,
                    onBack = { navController.popBackStack() },
                    onSaveMeal = { name, group, ingredients ->
                        viewModel.addMeal(name, group, ingredients)
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.ShoppingList.route) {
                ShoppingListScreen(
                    ingredients = viewModel.getAggregatedShoppingList(),
                    dayCount = dayCount,
                    isIngredientPurchased = viewModel::isIngredientPurchased,
                    onIngredientPurchasedChange = viewModel::setIngredientPurchased,
                    onBack = { navController.popBackStack() },
                    onClear = viewModel::clearShoppingSelection,
                    onDayCountChange = viewModel::updateDayCount,
                    onShareViaViber = {
                        shareToApp(
                            context = context,
                            packageName = "com.viber.voip",
                            message = viewModel.buildShoppingListMessage(),
                            fallbackLabel = "Вайбер"
                        )
                    },
                    onShareViaTelegram = {
                        shareToApp(
                            context = context,
                            packageName = "org.telegram.messenger",
                            message = viewModel.buildShoppingListMessage(),
                            fallbackLabel = "Телеграм"
                        )
                    },
                    onSavePdf = {
                        savePdfLauncher.launch("shopping-list-${System.currentTimeMillis()}.pdf")
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

private fun shareToApp(
    context: android.content.Context,
    packageName: String,
    message: String,
    fallbackLabel: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        setPackage(packageName)
    }

    runCatching {
        context.startActivity(intent)
    }.recoverCatching {
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            },
            "Выберите приложение"
        )
        context.startActivity(chooser)
    }.onFailure {
        val messageText = if (it is ActivityNotFoundException) {
            "$fallbackLabel недоступен"
        } else {
            "Не удалось отправить"
        }
        Toast.makeText(context, messageText, Toast.LENGTH_LONG).show()
    }
}
