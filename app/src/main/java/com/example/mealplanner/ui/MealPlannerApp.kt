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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mealplanner.ui.navigation.Screen
import com.example.mealplanner.ui.presentation.ProvideUiDensity
import com.example.mealplanner.ui.screens.AddMealScreen
import com.example.mealplanner.ui.screens.MenuScreen
import com.example.mealplanner.ui.screens.SettingsScreen
import com.example.mealplanner.ui.screens.ShoppingListScreen
import com.example.mealplanner.ui.screens.WeeklyPlannerScreen
import com.example.mealplanner.viewmodel.AppViewModel
import com.example.mealplanner.viewmodel.MealPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerApp(
    mealPlannerViewModel: MealPlannerViewModel,
    appViewModel: AppViewModel
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val meals by mealPlannerViewModel.meals.collectAsState()
    val filteredMeals by mealPlannerViewModel.filteredMeals.collectAsState()
    val mealFilters by mealPlannerViewModel.mealFilters.collectAsState()
    val mealFilterOptions by mealPlannerViewModel.menuMealFilterOptions.collectAsState()
    val groups by mealPlannerViewModel.groups.collectAsState()
    val weeklyPlan by mealPlannerViewModel.weeklyPlan.collectAsState()
    val settings by appViewModel.settings.collectAsState()
    val dayCount by mealPlannerViewModel.dayCount.collectAsState()
    val purchasedIngredientKeys by mealPlannerViewModel.purchasedIngredientKeys.collectAsState()
    val addMealState by mealPlannerViewModel.addMealUiState.collectAsState()
    val groupedFilteredIngredientCatalog by mealPlannerViewModel.groupedFilteredIngredientCatalog.collectAsState()
    val undoUiState by mealPlannerViewModel.undoUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undoUiState?.id) {
        val currentUndoState = undoUiState ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = currentUndoState.message,
            actionLabel = currentUndoState.actionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Short
        )

        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            mealPlannerViewModel.undoLastRemoval()
        } else {
            mealPlannerViewModel.dismissUndoState()
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val saved = mealPlannerViewModel.saveShoppingListPdfToUri(uri)
        Toast.makeText(
            context,
            if (saved) "PDF сохранён" else "Не удалось сохранить PDF",
            Toast.LENGTH_LONG
        ).show()
    }


    val saveDatabaseExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        mealPlannerViewModel.exportDatabaseToUri(uri) { saved ->
            Toast.makeText(
                context,
                if (saved) "База данных экспортирована" else "Не удалось экспортировать базу данных",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    val importMode = remember { androidx.compose.runtime.mutableStateOf(false) }

    val importDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        mealPlannerViewModel.importDatabaseFromUri(uri, overwritePlanner = importMode.value) { result ->
            val message = if (result.isSuccess) {
                "Импорт завершён: блюд ${result.importedMeals}, ингредиентов ${result.importedIngredients}"
            } else {
                "Ошибка импорта: ${result.error ?: "неизвестная ошибка"}"
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val destinations = listOf(Screen.Menu, Screen.AddMeal, Screen.WeeklyPlanner, Screen.ShoppingList, Screen.Settings)

    ProvideUiDensity(densityMode = settings.densityMode) {
        Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            TopAppBar(title = {
                Text(
                    when (currentRoute) {
                        Screen.Menu.route -> "Меню"
                        Screen.AddMeal.route -> "Добавить блюдо"
                        Screen.WeeklyPlanner.route -> "План недели"
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
                    meals = filteredMeals,
                    groups = groups,
                    mealFilterState = mealFilters,
                    mealFilterOptions = mealFilterOptions,
                    onRemoveMeal = mealPlannerViewModel::removeMeal,
                    onMoveMealToGroup = mealPlannerViewModel::moveMealToGroup,
                    onDuplicateMealToGroup = mealPlannerViewModel::duplicateMealToGroup,
                    onCreateGroup = mealPlannerViewModel::addGroup,
                    onDeleteGroup = mealPlannerViewModel::removeGroup,
                    onEditGroup = mealPlannerViewModel::renameGroup,
                    onMealFilterGroupSelect = mealPlannerViewModel::updateMealFilterGroup,
                    onMealFilterIngredientSelect = mealPlannerViewModel::updateMealFilterIngredient,
                    onMealFilterCategorySelect = mealPlannerViewModel::updateMealFilterCategory,
                    onClearMealFilters = mealPlannerViewModel::clearMealFilters,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToShopping = { navController.navigate(Screen.ShoppingList.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.AddMeal.route) {
                LaunchedEffect(groups) {
                    mealPlannerViewModel.onAddMealScreenVisible(groups)
                }
                AddMealScreen(
                    groups = groups,
                    groupedFilteredIngredients = groupedFilteredIngredientCatalog,
                    state = addMealState,
                    onBack = { navController.popBackStack() },
                    onMealNameChange = mealPlannerViewModel::updateAddMealName,
                    onGroupSelect = mealPlannerViewModel::updateAddMealGroup,
                    onMealTypeSelect = mealPlannerViewModel::updateAddMealType,
                    onStepChange = mealPlannerViewModel::updateAddMealStep,
                    onOpenIngredientSheet = mealPlannerViewModel::openIngredientSheet,
                    onCloseIngredientSheet = mealPlannerViewModel::closeIngredientSheet,
                    onIngredientSearchChange = mealPlannerViewModel::updateIngredientSearchQuery,
                    onIngredientSelect = mealPlannerViewModel::selectIngredientFromCatalog,
                    onIngredientUnitChange = mealPlannerViewModel::updateIngredientUnitInput,
                    onIngredientQuantityChange = mealPlannerViewModel::updateIngredientQuantityInput,
                    onConfirmIngredient = { mealPlannerViewModel.confirmIngredientFromSheet() },
                    onEditDraftIngredient = mealPlannerViewModel::editDraftIngredient,
                    onRemoveDraftIngredient = mealPlannerViewModel::removeDraftIngredient,
                    onReorderDraftIngredient = mealPlannerViewModel::reorderDraftIngredient,
                    onSaveMeal = {
                        mealPlannerViewModel.saveMealFromDraft {
                            navController.popBackStack()
                        }
                    }
                )
            }
            composable(Screen.WeeklyPlanner.route) {
                WeeklyPlannerScreen(
                    meals = meals,
                    weeklyPlan = weeklyPlan,
                    onAssignMeal = mealPlannerViewModel::assignMealToSlot
                )
            }
            composable(Screen.ShoppingList.route) {
                ShoppingListScreen(
                    ingredients = mealPlannerViewModel.getAggregatedShoppingList(),
                    categoriesByStorageKey = mealPlannerViewModel.getShoppingIngredientCategoriesByStorageKey(),
                    dayCount = dayCount,
                    purchasedIngredientKeys = purchasedIngredientKeys,
                    onIngredientPurchasedChange = mealPlannerViewModel::setIngredientPurchased,
                    onRemoveIngredient = mealPlannerViewModel::removeShoppingIngredient,
                    onBack = { navController.popBackStack() },
                    onClear = mealPlannerViewModel::clearShoppingSelection,
                    onDayCountChange = mealPlannerViewModel::updateDayCount,
                    onSend = {
                        sharePdf(context, mealPlannerViewModel)
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
                    onPersistDataToggle = mealPlannerViewModel::updatePersistDataBetweenLaunches,
                    onClearAfterExportToggle = mealPlannerViewModel::updateClearShoppingAfterExport,
                    onThemeModeSelect = appViewModel::updateThemeMode,
                    onAccentPaletteSelect = appViewModel::updateAccentPalette,
                    onDensityModeSelect = appViewModel::updateDensityMode,
                    onExportDatabase = {
                        saveDatabaseExportLauncher.launch("meal-planner-export-${System.currentTimeMillis()}.json")
                    },
                    onImportDatabaseMerge = {
                        importMode.value = false
                        importDatabaseLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
                    },
                    onImportDatabaseOverwrite = {
                        importMode.value = true
                        importDatabaseLauncher.launch(arrayOf("application/json", "text/json", "*/*"))
                    }
                )
            }
        }
    }
}

}

private fun iconFor(screen: Screen): String = when (screen) {
    Screen.Menu -> "📋"
    Screen.AddMeal -> "➕"
    Screen.WeeklyPlanner -> "📅"
    Screen.ShoppingList -> "🛒"
    Screen.Settings -> "⚙️"
}

private fun labelFor(screen: Screen): String = when (screen) {
    Screen.Menu -> "Меню"
    Screen.AddMeal -> "Добавить"
    Screen.WeeklyPlanner -> "Неделя"
    Screen.ShoppingList -> "Покупки"
    Screen.Settings -> "Настройки"
}


private fun sharePdf(
    context: android.content.Context,
    mealPlannerViewModel: MealPlannerViewModel
) {
    val pdfFile = mealPlannerViewModel.createSharePdfFile()
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
        putExtra(Intent.EXTRA_TEXT, mealPlannerViewModel.buildShoppingListMessage())
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
