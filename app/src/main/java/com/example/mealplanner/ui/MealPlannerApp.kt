package com.example.mealplanner.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
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
import com.example.mealplanner.ui.screens.WeeklyPlannerScreen
import com.example.mealplanner.viewmodel.MealPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerApp(viewModel: MealPlannerViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val meals by viewModel.meals.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val weeklyPlan by viewModel.weeklyPlan.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val dayCount by viewModel.dayCount.collectAsState()
    val purchasedIngredientKeys by viewModel.purchasedIngredientKeys.collectAsState()

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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val currentScreen = Screen.bottomNavigationItems.firstOrNull { it.route == currentRoute }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(currentScreen?.title ?: "Meal Planner")
            })
        },
        bottomBar = {
            NavigationBar {
                Screen.bottomNavigationItems.forEach { screen ->
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
                        icon = { Text(screen.icon) },
                        label = { Text(screen.label) }
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
                    onRemoveMeal = viewModel::removeMeal,
                    onMoveMealToGroup = viewModel::moveMealToGroup,
                    onDuplicateMealToGroup = viewModel::duplicateMealToGroup,
                    onCreateGroup = viewModel::addGroup,
                    onDeleteGroup = viewModel::removeGroup,
                    onEditGroup = viewModel::renameGroup,
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
            composable(Screen.WeeklyPlanner.route) {
                WeeklyPlannerScreen(
                    meals = meals,
                    weeklyPlan = weeklyPlan,
                    onAssignMeal = viewModel::assignMealToSlot
                )
            }
            composable(Screen.ShoppingList.route) {
                ShoppingListScreen(
                    ingredients = viewModel.getAggregatedShoppingList(),
                    dayCount = dayCount,
                    purchasedIngredientKeys = purchasedIngredientKeys,
                    onIngredientPurchasedChange = viewModel::setIngredientPurchased,
                    onBack = { navController.popBackStack() },
                    onClear = viewModel::clearShoppingSelection,
                    onDayCountChange = viewModel::updateDayCount,
                    onSend = {
                        sharePdf(context, viewModel)
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

private fun sharePdf(
    context: android.content.Context,
    viewModel: MealPlannerViewModel
) {
    val pdfFile = viewModel.createSharePdfFile()
    if (pdfFile == null) {
        Toast.makeText(context, "Список покупок пуст", Toast.LENGTH_LONG).show()
        return
    }

    val authority = "${context.packageName}.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Список покупок")
        putExtra(Intent.EXTRA_TEXT, viewModel.buildShoppingListMessage())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, "Отправить список"))
    }.onFailure {
        val messageText = if (it is ActivityNotFoundException) {
            "Приложения для отправки не найдены"
        } else {
            "Не удалось отправить"
        }
        Toast.makeText(context, messageText, Toast.LENGTH_LONG).show()
    }
}
