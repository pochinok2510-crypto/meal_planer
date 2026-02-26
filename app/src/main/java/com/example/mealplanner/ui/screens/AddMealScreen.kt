package com.example.mealplanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mealplanner.data.IngredientRepository
import com.example.mealplanner.data.local.Ingredient
import com.example.mealplanner.data.local.IngredientGroup
import com.example.mealplanner.ui.presentation.toRussianUnitLabel
import com.example.mealplanner.viewmodel.AddMealStep
import com.example.mealplanner.viewmodel.AddMealUiState

private val SUPPORTED_UNITS = listOf("g", "kg", "ml", "l", "pcs", "tsp", "tbsp", "pack")
private val MEAL_TYPES = listOf("Завтрак", "Обед", "Ужин", "Перекус")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddMealScreen(
    groups: List<String>,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    ingredientGroups: List<IngredientGroup>,
    state: AddMealUiState,
    animationsEnabled: Boolean,
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
    onIngredientGroupSelect: (String) -> Unit,
    onCreateIngredientGroup: (String) -> Boolean,
    onDeleteIngredientGroup: (String) -> Unit,
    onDeleteCatalogIngredient: (Long) -> Unit,
    onConfirmIngredient: () -> Unit,
    onEditDraftIngredient: (String) -> Unit,
    onRemoveDraftIngredient: (String) -> Unit,
    onReorderDraftIngredient: (Int, Int) -> Unit,
    onSaveMeal: () -> Unit
) {
    var groupMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var mealTypeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var removingIngredientIds by remember { mutableStateOf(setOf<String>()) }

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
                    val dragListState = rememberLazyListState()
                    var draggingIndex by remember { mutableStateOf<Int?>(null) }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = dragListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = state.selectedIngredients,
                            key = { _, item -> item.id }
                        ) { index, item ->

                            val visibilityState = remember(item.id) {
                                MutableTransitionState(false).apply { targetState = true }
                            }

                            LaunchedEffect(removingIngredientIds.contains(item.id)) {
                                visibilityState.targetState = !removingIngredientIds.contains(item.id)
                            }

                            val removeRequested = item.id in removingIngredientIds

                            if (animationsEnabled) {
                                AnimatedVisibility(
                                    visibleState = visibilityState,
                                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                                    exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180))
                                ) {
                                    DraftIngredientCard(
                                        item = item,
                                        selectedCount = state.selectedIngredients.size,
                                        dragListState = dragListState,
                                        index = index,
                                        draggingIndex = draggingIndex,
                                        onDraggingIndexChange = { draggingIndex = it },
                                        onReorderDraftIngredient = onReorderDraftIngredient,
                                        onEditDraftIngredient = onEditDraftIngredient,
                                        onRequestRemove = {
                                            if (item.id !in removingIngredientIds) {
                                                removingIngredientIds = removingIngredientIds + item.id
                                            }
                                        }
                                    )
                                }
                            } else if (!removeRequested) {
                                DraftIngredientCard(
                                    item = item,
                                    selectedCount = state.selectedIngredients.size,
                                    dragListState = dragListState,
                                    index = index,
                                    draggingIndex = draggingIndex,
                                    onDraggingIndexChange = { draggingIndex = it },
                                    onReorderDraftIngredient = onReorderDraftIngredient,
                                    onEditDraftIngredient = onEditDraftIngredient,
                                    onRequestRemove = {
                                        if (item.id !in removingIngredientIds) {
                                            removingIngredientIds = removingIngredientIds + item.id
                                        }
                                    }
                                )
                            }

                            if (removeRequested) {
                                if (animationsEnabled) {
                                    if (visibilityState.isIdle && !visibilityState.currentState) {
                                        LaunchedEffect(item.id) {
                                            delay(40)
                                            onRemoveDraftIngredient(item.id)
                                            removingIngredientIds = removingIngredientIds - item.id
                                        }
                                    }
                                } else {
                                    LaunchedEffect(item.id) {
                                        onRemoveDraftIngredient(item.id)
                                        removingIngredientIds = removingIngredientIds - item.id
                                    }
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
            ingredientGroups = ingredientGroups,
            onUnitChange = onIngredientUnitChange,
            onQuantityChange = onIngredientQuantityChange,
            onGroupSelect = onIngredientGroupSelect,
            onCreateGroup = onCreateIngredientGroup,
            onDeleteGroup = onDeleteIngredientGroup,
            onDeleteIngredient = onDeleteCatalogIngredient,
            onConfirm = onConfirmIngredient
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraftIngredientCard(
    item: com.example.mealplanner.viewmodel.MealIngredientDraft,
    selectedCount: Int,
    dragListState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    draggingIndex: Int?,
    onDraggingIndexChange: (Int?) -> Unit,
    onReorderDraftIngredient: (Int, Int) -> Unit,
    onEditDraftIngredient: (String) -> Unit,
    onRequestRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(selectedCount) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDraggingIndexChange(index)
                    },
                    onDragEnd = {
                        onDraggingIndexChange(null)
                    },
                    onDragCancel = {
                        onDraggingIndexChange(null)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y == 0f) return@detectDragGesturesAfterLongPress

                        val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                        val currentItem = dragListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == currentIndex }
                            ?: return@detectDragGesturesAfterLongPress

                        val currentCenter = currentItem.offset + (currentItem.size / 2) + dragAmount.y
                        val target = dragListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { info ->
                                currentCenter.toInt() in info.offset..(info.offset + info.size)
                            }
                            ?: return@detectDragGesturesAfterLongPress

                        if (target.index != currentIndex) {
                            onReorderDraftIngredient(currentIndex, target.index)
                            onDraggingIndexChange(target.index)
                        }
                    }
                )
            }

    ) {
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
                Text(text = "${item.quantityInput} ${item.unit.toRussianUnitLabel()}")
            }
            Text("⋮⋮")
            IconButton(onClick = { onEditDraftIngredient(item.id) }) {
                Text("✏️")
            }
            IconButton(onClick = onRequestRemove) {
                Text("🗑️")
            }

        }
    }

    groupToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Удалить группу?") },
            text = { Text("Все ингредиенты из '${target.name}' будут перемещены в '${IngredientRepository.DEFAULT_GROUP_NAME}'.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(target.id)
                    groupToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Отмена") }
            }
        )
    }

    ingredientToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { ingredientToDelete = null },
            title = { Text("Удалить ингредиент?") },
            text = { Text(target.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteIngredient(target.id)
                    ingredientToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { ingredientToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientSheet(
    state: AddMealUiState,
    groupedFilteredIngredients: Map<String, List<Ingredient>>,
    ingredientGroups: List<IngredientGroup>,
    onDismiss: () -> Unit,
    onIngredientSearchChange: (String) -> Unit,
    onIngredientSelect: (String, String) -> Unit,
    onUnitChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onGroupSelect: (String) -> Unit,
    onCreateGroup: (String) -> Boolean,
    onDeleteGroup: (String) -> Unit,
    onDeleteIngredient: (Long) -> Unit,
    onConfirm: () -> Unit
) {
    var unitExpanded by remember { mutableStateOf(false) }
    var ingredientGroupExpanded by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var groupToDelete by remember { mutableStateOf<IngredientGroup?>(null) }
    var ingredientToDelete by remember { mutableStateOf<Ingredient?>(null) }
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

            ExposedDropdownMenuBox(
                expanded = ingredientGroupExpanded,
                onExpandedChange = { ingredientGroupExpanded = !ingredientGroupExpanded }
            ) {
                val selectedGroupName = ingredientGroups.firstOrNull { it.id == state.ingredientGroupId }?.name
                    ?: IngredientRepository.DEFAULT_GROUP_NAME
                OutlinedTextField(
                    value = selectedGroupName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Группа ингредиента") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingredientGroupExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = ingredientGroupExpanded,
                    onDismissRequest = { ingredientGroupExpanded = false }
                ) {
                    ingredientGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                onGroupSelect(group.id)
                                ingredientGroupExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Новая группа") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (onCreateGroup(newGroupName)) {
                        newGroupName = ""
                    }
                }) {
                    Text("Создать")
                }
            }

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
                            text = category,
                            modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                    items(items = ingredients, key = { item -> item.id }) { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ingredient.name} (${ingredient.unit.toRussianUnitLabel()})",
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onIngredientSelect(ingredient.name, ingredient.unit)
                                    }
                                    .padding(vertical = 8.dp)
                            )
                            IconButton(onClick = { ingredientToDelete = ingredient }) {
                                Text("🗑️")
                            }
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded }
            ) {
                OutlinedTextField(
                    value = selectedUnit.toRussianUnitLabel(),
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
                            text = { Text(unit.toRussianUnitLabel()) },
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

            Text("Удаление групп")
            ingredientGroups.forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(group.name, modifier = Modifier.weight(1f))
                    val isDefault = group.name.equals(IngredientRepository.DEFAULT_GROUP_NAME, ignoreCase = true)
                    IconButton(onClick = { if (!isDefault) groupToDelete = group }, enabled = !isDefault) {
                        Text("🗑️")
                    }
                }
            }

            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить")
            }
        }
    }

    groupToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Удалить группу?") },
            text = { Text("Все ингредиенты из '${target.name}' будут перемещены в '${IngredientRepository.DEFAULT_GROUP_NAME}'.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(target.id)
                    groupToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Отмена") }
            }
        )
    }

    ingredientToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { ingredientToDelete = null },
            title = { Text("Удалить ингредиент?") },
            text = { Text(target.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteIngredient(target.id)
                    ingredientToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { ingredientToDelete = null }) { Text("Отмена") }
            }
        )
    }

}
