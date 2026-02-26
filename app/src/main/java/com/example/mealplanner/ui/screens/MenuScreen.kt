package com.example.mealplanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.mealplanner.data.MealsRepository
import com.example.mealplanner.model.Meal
import com.example.mealplanner.viewmodel.MealFilterOptions
import com.example.mealplanner.viewmodel.MealFilterState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuScreen(
    meals: List<Meal>,
    groups: List<String>,
    mealFilterState: MealFilterState,
    mealFilterOptions: MealFilterOptions,
    onRemoveMeal: (Meal) -> Unit,
    onMoveMealToGroup: (Meal, String) -> Unit,
    onDuplicateMealToGroup: (Meal, String) -> Unit,
    onCreateGroup: (String) -> Boolean,
    onDeleteGroup: (String) -> Unit,
    onEditGroup: (String, String) -> Boolean,
    onMealFilterGroupSelect: (String?) -> Unit,
    onMealFilterIngredientSelect: (String?) -> Unit,
    onMealFilterCategorySelect: (String?) -> Unit,
    onClearMealFilters: () -> Unit,
    onNavigateToAddMeal: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var newGroupName by remember { mutableStateOf("") }
    var groupError by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var groupPendingDelete by remember { mutableStateOf<String?>(null) }
    var groupPendingEdit by remember { mutableStateOf<String?>(null) }
    var editGroupName by remember { mutableStateOf("") }
    var expandedGroups by rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isFilterSheetVisible by rememberSaveable { mutableStateOf(false) }

    val normalizedSearch = searchQuery.trim().lowercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск блюд") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isFilterSheetVisible = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Фильтры")
            }
            TextButton(
                onClick = onClearMealFilters,
                modifier = Modifier.weight(1f)
            ) {
                Text("Сбросить фильтры")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = {
                    newGroupName = it
                    groupError = null
                },
                label = { Text("Новая группа") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    val result = onCreateGroup(newGroupName)
                    if (result) {
                        newGroupName = ""
                        groupError = null
                    } else {
                        groupError = "Не удалось добавить группу"
                    }
                }
            ) {
                Text("Добавить")
            }
        }

        groupError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                val mealsInGroup = meals.filter { meal ->
                    meal.group == group && (normalizedSearch.isBlank() || meal.name.lowercase().contains(normalizedSearch))
                }
                val isExpanded = expandedGroups[group] ?: true

                item(key = "group_$group") {
                    GroupHeader(
                        group = group,
                        mealCount = mealsInGroup.size,
                        isExpanded = isExpanded,
                        canManage = group !in MealsRepository.DEFAULT_GROUPS && group != MealsRepository.UNCATEGORIZED_GROUP,
                        onToggleExpanded = {
                            expandedGroups = expandedGroups + (group to !isExpanded)
                        },
                        onEdit = {
                            groupPendingEdit = group
                            editGroupName = group
                        },
                        onDelete = { groupPendingDelete = group }
                    )
                }

                items(mealsInGroup, key = { it.id }) { meal ->
                    AnimatedVisibility(visible = isExpanded) {
                        MealCard(
                            modifier = Modifier.padding(start = 12.dp),
                            meal = meal,
                            groups = groups,
                            onRemove = { onRemoveMeal(meal) },
                            onMove = { target -> onMoveMealToGroup(meal, target) },
                            onDuplicate = { target -> onDuplicateMealToGroup(meal, target) }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onNavigateToAddMeal, modifier = Modifier.weight(1f)) {
                Text("➕ Добавить блюдо")
            }
            Button(onClick = onNavigateToShopping, modifier = Modifier.weight(1f)) {
                Text("🛒 Покупки")
            }
        }

        TextButton(onClick = onNavigateToSettings, modifier = Modifier.align(Alignment.End)) {
            Text("⚙️ Настройки")
        }
    }



    if (isFilterSheetVisible) {
        MealFilterBottomSheet(
            state = mealFilterState,
            options = mealFilterOptions,
            onGroupSelect = onMealFilterGroupSelect,
            onIngredientSelect = onMealFilterIngredientSelect,
            onCategorySelect = onMealFilterCategorySelect,
            onDismiss = { isFilterSheetVisible = false }
        )
    }

    groupPendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            title = { Text("Удалить группу?") },
            text = { Text("Группа '$group' будет удалена. Блюда останутся в " + MealsRepository.UNCATEGORIZED_GROUP + ".") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(group)
                    groupPendingDelete = null
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingDelete = null }) { Text("Отмена") }
            }
        )
    }

    groupPendingEdit?.let { group ->
        AlertDialog(
            onDismissRequest = { groupPendingEdit = null },
            title = { Text("Переименовать группу") },
            text = {
                OutlinedTextField(
                    value = editGroupName,
                    onValueChange = { editGroupName = it },
                    singleLine = true,
                    label = { Text("Новое название") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val result = onEditGroup(group, editGroupName)
                    if (result) {
                        groupPendingEdit = null
                        groupError = null
                    } else {
                        groupError = "Не удалось переименовать группу"
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { groupPendingEdit = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealFilterBottomSheet(
    state: MealFilterState,
    options: MealFilterOptions,
    onGroupSelect: (String?) -> Unit,
    onIngredientSelect: (String?) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var groupExpanded by remember { mutableStateOf(false) }
    var ingredientExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterDropdownField(
                label = "Группа",
                selectedValue = state.selectedGroup,
                expanded = groupExpanded,
                values = options.groups,
                onExpandedChange = { groupExpanded = !groupExpanded },
                onDismissRequest = { groupExpanded = false },
                onSelect = {
                    onGroupSelect(it)
                    groupExpanded = false
                }
            )
            FilterDropdownField(
                label = "Ингредиент",
                selectedValue = state.selectedIngredient,
                expanded = ingredientExpanded,
                values = options.ingredients,
                onExpandedChange = { ingredientExpanded = !ingredientExpanded },
                onDismissRequest = { ingredientExpanded = false },
                onSelect = {
                    onIngredientSelect(it)
                    ingredientExpanded = false
                }
            )
            FilterDropdownField(
                label = "Категория",
                selectedValue = state.selectedCategory,
                expanded = categoryExpanded,
                values = options.categories,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                onDismissRequest = { categoryExpanded = false },
                onSelect = {
                    onCategorySelect(it)
                    categoryExpanded = false
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FilterDropdownField(
    label: String,
    selectedValue: String?,
    expanded: Boolean,
    values: List<String>,
    onExpandedChange: () -> Unit,
    onDismissRequest: () -> Unit,
    onSelect: (String?) -> Unit
) {
    Box {
        OutlinedTextField(
            value = selectedValue ?: "Все",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandedChange)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            DropdownMenuItem(text = { Text("Все") }, onClick = { onSelect(null) })
            values.forEach { value ->
                DropdownMenuItem(text = { Text(value) }, onClick = { onSelect(value) })
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: String,
    mealCount: Int,
    isExpanded: Boolean,
    canManage: Boolean,
    onToggleExpanded: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = groupAccentColor(group)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = lerp(accentColor, MaterialTheme.colorScheme.surface, 0.82f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            TextButton(onClick = onToggleExpanded) {
                Text(if (isExpanded) "▲" else "▼")
            }
            Text(
                text = "$group ($mealCount)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (canManage) {
                TextButton(onClick = onEdit) {
                    Text("Изм.")
                }
                TextButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    modifier: Modifier = Modifier,
    meal: Meal,
    groups: List<String>,
    onRemove: () -> Unit,
    onMove: (String) -> Unit,
    onDuplicate: (String) -> Unit
) {
    var moveExpanded by remember { mutableStateOf(false) }
    var duplicateExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onRemove
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = meal.name, style = MaterialTheme.typography.titleMedium)
                    Text("Ингредиентов: ${meal.ingredients.size}")
                }
                IconButton(onClick = onRemove) {
                    Text("🗑")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { moveExpanded = true }, modifier = Modifier.weight(1f)) {
                    Text("Переместить")
                }
                Button(onClick = { duplicateExpanded = true }, modifier = Modifier.weight(1f)) {
                    Text("Дублировать")
                }
            }

            DropdownMenu(expanded = moveExpanded, onDismissRequest = { moveExpanded = false }) {
                groups.filterNot { it == meal.group }.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onMove(group)
                            moveExpanded = false
                        }
                    )
                }
            }

            DropdownMenu(expanded = duplicateExpanded, onDismissRequest = { duplicateExpanded = false }) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            onDuplicate(group)
                            duplicateExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun groupAccentColor(group: String): Color {
    val accents = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )
    val index = group.hashCode().mod(accents.size)
    return accents[index]
}
