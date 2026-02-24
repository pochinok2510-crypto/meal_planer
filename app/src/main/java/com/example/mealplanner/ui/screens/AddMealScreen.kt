package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.data.local.Ingredient
import com.example.mealplanner.viewmodel.AddMealStep
import com.example.mealplanner.viewmodel.AddMealUiState

private const val INGREDIENT_OTHER_CATEGORY = "Other"
private val SUPPORTED_UNITS = listOf("g", "kg", "ml", "l", "pcs", "tsp", "tbsp", "pack")
private val MEAL_TYPES = listOf("Завтрак", "Обед", "Ужин", "Перекус")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    groups: List<String>,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    state: AddMealUiState,
    onBack: () -> Unit,
    onMealNameChange: (String) -> Unit,
    onGroupSelect: (String) -> Unit,
    onMealTypeSelect: (String) -> Unit,
    onStepChange: (AddMealStep) -> Unit,
    onOpenIngredientSheet: () -> Unit,
    onCloseIngredientSheet: () -> Unit,
    onIngredientSearchChange: (String) -> Unit,
    onIngredientSelect: (String, String) -> Unit,
    onIngredientUnitChange: (String) -> Unit,
    onIngredientQuantityChange: (String) -> Unit,
    onConfirmIngredient: () -> Unit,
    onEditDraftIngredient: (String) -> Unit,
    onRemoveDraftIngredient: (String) -> Unit,
    onSaveMeal: () -> Unit
) {
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var mealTypeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var removingIngredientIds by remember { mutableStateOf(setOf<String>()) }
    val selectedIngredientsListState = rememberSaveable(saver = lazyListStateSaver()) {
        LazyListState()
    }
    val tabs = listOf(AddMealStep.BASIC_INFO to "Шаг 1: Основное", AddMealStep.INGREDIENTS to "Шаг 2: Ингредиенты")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == state.selectedStep }) {
            tabs.forEach { (step, title) ->
                Tab(
                    selected = state.selectedStep == step,
                    onClick = { onStepChange(step) },
                    text = { Text(title) }
                )
            }
        }

        when (state.selectedStep) {
            AddMealStep.BASIC_INFO -> {
                OutlinedTextField(
                    value = state.mealName,
                    onValueChange = onMealNameChange,
                    label = { Text("Название блюда") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = groupMenuExpanded,
                    onExpandedChange = { groupMenuExpanded = !groupMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.selectedGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Группа") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = groupMenuExpanded,
                        onDismissRequest = { groupMenuExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = {
                                    onGroupSelect(group)
                                    groupMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = mealTypeMenuExpanded,
                    onExpandedChange = { mealTypeMenuExpanded = !mealTypeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.mealType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип приема пищи") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = mealTypeMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    androidx.compose.material3.DropdownMenu(
                        expanded = mealTypeMenuExpanded,
                        onDismissRequest = { mealTypeMenuExpanded = false }
                    ) {
                        MEAL_TYPES.forEach { mealType ->
                            DropdownMenuItem(
                                text = { Text(mealType) },
                                onClick = {
                                    onMealTypeSelect(mealType)
                                    mealTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            AddMealStep.INGREDIENTS -> {
                Text("Ингредиенты")

                Button(onClick = onOpenIngredientSheet, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Добавить ингредиент")
                }

                if (state.selectedIngredients.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = selectedIngredientsListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.selectedIngredients,
                            key = { item -> item.id }
                        ) { item ->
                            val visibilityState = remember(item.id) {
                                MutableTransitionState(false).apply { targetState = true }
                            }

                            LaunchedEffect(removingIngredientIds.contains(item.id)) {
                                visibilityState.targetState = !removingIngredientIds.contains(item.id)
                            }

                            AnimatedVisibility(
                                visibleState = visibilityState,
                                enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
                            ) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = item.name)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "${item.quantityInput} ${item.unit}")
                                        }
                                        IconButton(onClick = { onEditDraftIngredient(item.id) }) {
                                            Text("✏️")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (item.id !in removingIngredientIds) {
                                                    removingIngredientIds = removingIngredientIds + item.id
                                                }
                                            }
                                        ) {
                                            Text("🗑️")
                                        }
                                    }
                                }
                            }

                            if (item.id in removingIngredientIds && visibilityState.isIdle && !visibilityState.currentState) {
                                LaunchedEffect(item.id) {
                                    delay(40)
                                    onRemoveDraftIngredient(item.id)
                                    removingIngredientIds = removingIngredientIds - item.id
                                }
                            }
                        }
                    }
                }
            }
        }

        state.error?.let {
            Text(it)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) {
                Text("← Назад")
            }
            Button(onClick = onSaveMeal) {
                Text("Сохранить блюдо")
            }
        }
    }

    if (state.isIngredientSheetVisible) {
        IngredientSheet(
            state = state,
            groupedFilteredIngredients = groupedFilteredIngredients,
            onDismiss = onCloseIngredientSheet,
            onIngredientSearchChange = onIngredientSearchChange,
            onIngredientSelect = onIngredientSelect,
            onUnitChange = onIngredientUnitChange,
            onQuantityChange = onIngredientQuantityChange,
            onConfirm = onConfirmIngredient
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientSheet(
    state: AddMealUiState,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    onDismiss: () -> Unit,
    onIngredientSearchChange: (String) -> Unit,
    onIngredientSelect: (String, String) -> Unit,
    onUnitChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    var unitExpanded by remember { mutableStateOf(false) }
    val selectedUnit = state.ingredientUnitInput
    val availableUnits = remember(selectedUnit) {
        if (selectedUnit.isBlank() || SUPPORTED_UNITS.contains(selectedUnit)) SUPPORTED_UNITS
        else listOf(selectedUnit) + SUPPORTED_UNITS
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Добавление ингредиента")

            OutlinedTextField(
                value = state.ingredientSearchQuery,
                onValueChange = onIngredientSearchChange,
                label = { Text("Поиск или новый ингредиент") },
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedFilteredIngredients.forEach { (category, ingredients) ->
                    item(key = "header_$category") {
                        Text(
                            text = category.ifBlank { INGREDIENT_OTHER_CATEGORY },
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                    items(items = ingredients, key = { item -> item.id }) { ingredient ->
                        Text(
                            text = "${ingredient.name} (${ingredient.unit})",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIngredientSelect(ingredient.name, ingredient.unit)
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUnit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Единица") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    availableUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                onUnitChange(unit)
                                unitExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.ingredientQuantityInput,
                onValueChange = onQuantityChange,
                label = { Text("Количество") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить")
            }
        }
    }
}

private fun lazyListStateSaver(): Saver<LazyListState, List<Int>> {
    return Saver(
        save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
        restore = { restored ->
            LazyListState(
                firstVisibleItemIndex = restored.getOrElse(0) { 0 },
                firstVisibleItemScrollOffset = restored.getOrElse(1) { 0 }
            )
        }
    )
}
