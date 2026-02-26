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
    val filteredMeals by viewModel.filteredMeals.collectAsState()
    val mealFilters by viewModel.mealFilters.collectAsState()
    val mealFilterOptions by viewModel.menuMealFilterOptions.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val weeklyPlan by viewModel.weeklyPlan.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val dayCount by viewModel.dayCount.collectAsState()
    val purchasedIngredientKeys by viewModel.purchasedIngredientKeys.collectAsState()
    val addMealState by viewModel.addMealUiState.collectAsState()
    val groupedFilteredIngredientCatalog by viewModel.groupedFilteredIngredientCatalog.collectAsState()
    val undoUiState by viewModel.undoUiState.collectAsState()
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
            viewModel.undoLastRemoval()
        } else {
            viewModel.dismissUndoState()
        }
    }

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


    val saveDatabaseExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        viewModel.exportDatabaseToUri(uri) { saved ->
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

        viewModel.importDatabaseFromUri(uri, overwritePlanner = importMode.value) { result ->
            val message = if (result.isSuccess) {
                "Импорт завершён: блюд ${result.importedMeals}, ингредиентов ${result.importedIngredients}"
            } else {
                "Ошибка импорта: ${result.error ?: "неизвестная ошибка"}"
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val destinations = listOf(Screen.Menu, Screen.AddMeal, Screen.WeeklyPlanner, Screen.ShoppingList, Screen.Settings)

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
                    onRemoveMeal = viewModel::removeMeal,
                    onMoveMealToGroup = viewModel::moveMealToGroup,
                    onDuplicateMealToGroup = viewModel::duplicateMealToGroup,
                    onCreateGroup = viewModel::addGroup,
                    onDeleteGroup = viewModel::removeGroup,
                    onEditGroup = viewModel::renameGroup,
                    onMealFilterGroupSelect = viewModel::updateMealFilterGroup,
                    onMealFilterIngredientSelect = viewModel::updateMealFilterIngredient,
                    onMealFilterCategorySelect = viewModel::updateMealFilterCategory,
                    onClearMealFilters = viewModel::clearMealFilters,
                    onNavigateToAddMeal = { navController.navigate(Screen.AddMeal.route) },
                    onNavigateToShopping = { navController.navigate(Screen.ShoppingList.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.AddMeal.route) {
                LaunchedEffect(groups) {
                    viewModel.onAddMealScreenVisible(groups)
                }
                AddMealScreen(
                    groups = groups,
                    groupedFilteredIngredients = groupedFilteredIngredientCatalog,
                    state = addMealState,
                    onBack = { navController.popBackStack() },
                    onMealNameChange = viewModel::updateAddMealName,
                    onGroupSelect = viewModel::updateAddMealGroup,
                    onMealTypeSelect = viewModel::updateAddMealType,
                    onStepChange = viewModel::updateAddMealStep,
                    onOpenIngredientSheet = viewModel::openIngredientSheet,
                    onCloseIngredientSheet = viewModel::closeIngredientSheet,
                    onIngredientSearchChange = viewModel::updateIngredientSearchQuery,
                    onIngredientSelect = viewModel::selectIngredientFromCatalog,
                    onIngredientUnitChange = viewModel::updateIngredientUnitInput,
                    onIngredientQuantityChange = viewModel::updateIngredientQuantityInput,
                    onConfirmIngredient = { viewModel.confirmIngredientFromSheet() },
                    onEditDraftIngredient = viewModel::editDraftIngredient,
                    onRemoveDraftIngredient = viewModel::removeDraftIngredient,
                    onReorderDraftIngredient = viewModel::reorderDraftIngredient,
                    onSaveMeal = {
                        viewModel.saveMealFromDraft {
                            navController.popBackStack()
                        }
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
                    categoriesByStorageKey = viewModel.getShoppingIngredientCategoriesByStorageKey(),
                    dayCount = dayCount,
                    purchasedIngredientKeys = purchasedIngredientKeys,
                    onIngredientPurchasedChange = viewModel::setIngredientPurchased,
                    onRemoveIngredient = viewModel::removeShoppingIngredient,
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
                    onClearAfterExportToggle = viewModel::updateClearShoppingAfterExport,
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
